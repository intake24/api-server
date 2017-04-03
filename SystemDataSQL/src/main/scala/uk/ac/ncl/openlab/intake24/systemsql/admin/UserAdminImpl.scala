package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.sql.Connection
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class UserAdminImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends UserAdminService with SqlDataService with SqlResourceLoader {

  private case class RolesForId(userId: Int, roles: Set[String])

  private def updateUserRolesById(roles: Seq[RolesForId])(implicit connection: Connection): Either[RecordNotFound, Unit] = {

    SQL("DELETE FROM user_roles WHERE user_id IN (user_ids)")
      .on('user_ids -> roles.map(_.userId))
      .execute()

    val params = roles.flatMap {
      r =>
        r.roles.map {
          role => Seq[NamedParameter]('user_id -> r.userId, 'role -> role)
        }
    }

    if (!params.isEmpty) {
      tryWithConstraintCheck("user_roles_user_id_fkey", e => RecordNotFound(new RuntimeException(s"Could not update roles because one of the user records was not found"))) {
        BatchSql("INSERT INTO user_roles(user_id, role) VALUES ({user_id}, {role})", params.head, params.tail: _*).execute()
        Right(())
      }
    }

    Right(())
  }

  private case class RolesForAlias(surveyId: String, userName: String, roles: Set[String])

  private def updateUserRolesByAlias(roles: Seq[RolesForAlias])(implicit connection: Connection): Either[RecordNotFound, Unit] = {

    SQL("DELETE FROM user_roles WHERE user_id IN (SELECT user_id FROM user_survey_aliases WHERE (survey_id, user_name) IN (SELECT unnest(ARRAY[{survey_ids}]), unnest(ARRAY[{user_names}])))")
      .on('user_names -> roles.map(_.userName), 'survey_ids -> roles.map(_.surveyId))
      .execute()

    val params = roles.flatMap {
      r =>
        r.roles.map {
          role => Seq[NamedParameter]('survey_id -> r.surveyId, 'user_name -> r.userName, 'role -> role)
        }
    }

    if (!params.isEmpty) {
      tryWithConstraintCheck("user_roles_user_id_fkey", e => RecordNotFound(new RuntimeException(s"Could not update roles because one of the user records was not found"))) {
        BatchSql("INSERT INTO user_roles(user_id, role) VALUES (SELECT id FROM user_survey_aliases WHERE survey_id={survey_id} AND user_name={user_name}, {role})", params.head, params.tail: _*).execute()
        Right(())
      }
    }

    Right(())
  }

  private case class CustomDataForId(userId: Int, customData: Map[String, String])

  private def updateCustomDataById(customData: Seq[CustomDataForId])(implicit connection: Connection): Either[UpdateError, Unit] = {

    if (customData.nonEmpty) {

      SQL("DELETE FROM user_custom_fields WHERE user_id IN ({user_ids})")
        .on('user_ids -> customData.map(_.userId))
        .execute()

      val userCustomFieldParams = customData.flatMap {
        case r =>
          r.customData.map {
            case (name, value) => Seq[NamedParameter]('user_id -> r.userId, 'name -> name, 'value -> value)
          }
      }

      if (!userCustomFieldParams.isEmpty)
        tryWithConstraintCheck("user_custom_fields_user_id_fkey", e => RecordNotFound(new RuntimeException(s"Could not update custom user data because one of the user records was not found"))) {
          BatchSql("INSERT INTO user_custom_fields VALUES (DEFAULT, {user_id}, {name}, {value})", userCustomFieldParams.head, userCustomFieldParams.tail: _*).execute()
          Right(())
        }
      else
        Right(())
    } else
      Right(())
  }

  private case class CustomDataForAlias(surveyId: String, userName: String, customData: Map[String, String])

  private def updateCustomDataByAlias(customData: Seq[CustomDataForAlias])(implicit connection: Connection): Either[UpdateError, Unit] = {

    if (customData.nonEmpty) {

      SQL("DELETE FROM user_custom_fields WHERE user_id IN (SELECT user_id FROM user_survey_aliases WHERE (survey_id, user_name) IN (SELECT unnest(ARRAY[{survey_ids}]), unnest(ARRAY[{user_names}])))")
        .on('survey_ids -> customData.map(_.surveyId), 'user_names -> customData.map(_.userName))
        .execute()

      val userCustomFieldParams = customData.flatMap {
        case r =>
          r.customData.map {
            case (name, value) => Seq[NamedParameter]('survey_id -> r.surveyId, 'user_name -> r.userName, 'name -> name, 'value -> value)
          }
      }

      if (!userCustomFieldParams.isEmpty)
        tryWithConstraintCheck("user_custom_fields_user_id_fkey", e => RecordNotFound(new RuntimeException(s"Could not update custom user data because one of the user records was not found"))) {
          BatchSql("INSERT INTO user_custom_fields VALUES (DEFAULT, {user_id}, {name}, {value})", userCustomFieldParams.head, userCustomFieldParams.tail: _*).execute()
          Right(())
        }
      else
        Right(())
    } else
      Right(())
  }


  private lazy val createOrUpdateUserByAliasQuery = sqlFromResource("admin/users/create_or_update_user_by_alias.sql")

  def createOrUpdateUsersWithAliases(usersWithAliases: Seq[NewUserWithAlias]): Either[DependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val upsertParams = usersWithAliases.map {
          u =>
            Seq[NamedParameter]('survey_id -> u.alias.surveyId, 'user_name -> u.alias.userName,
              'password_hash -> u.password.hashBase64, 'password_salt -> u.password.saltBase64, 'password_hasher -> u.password.hasher,
              'name -> u.userInfo.name, 'email -> u.userInfo.email, 'phone -> u.userInfo.phone)
        }

        if (upsertParams.nonEmpty) {
          BatchSql(createOrUpdateUserByAliasQuery, upsertParams.head, upsertParams.tail: _*).execute()

          for (
            _ <- updateUserRolesByAlias(usersWithAliases.foldLeft(List[RolesForAlias]()) {
              case (acc, userRecord) => RolesForAlias(userRecord.alias.surveyId, userRecord.alias.userName, userRecord.userInfo.roles) +: acc
            }).right;
            _ <- updateCustomDataByAlias(usersWithAliases.foldLeft(List[CustomDataForAlias]()) {
              case (acc, record) => CustomDataForAlias(record.alias.surveyId, record.alias.userName, record.userInfo.customFields) +: acc
            }).right
          ) yield ()
        } else
          Right(())
      }
  }

  def createUser(newUser: NewUser): Either[CreateError, Int] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val userId = SQL("INSERT INTO users VALUES (DEFAULT, {password_hash}, {password_salt}, {password_hasher}, {name}, {email}, {phone})")
          .on('password_hash -> newUser.password.hashBase64,
            'password_salt -> newUser.password.saltBase64,
            'password_hasher -> newUser.password.hasher,
            'name -> newUser.userInfo.name,
            'email -> newUser.userInfo.email,
            'phone -> newUser.userInfo.phone)
          .executeInsert(SqlParser.scalar[Int].single)

        (for (
          _ <- updateUserRolesById(Seq(RolesForId(userId, newUser.userInfo.roles))).right;
          _ <- updateCustomDataById(Seq(CustomDataForId(userId, newUser.userInfo.customFields))).right
        ) yield userId).left.map(e => UnexpectedDatabaseError(e.exception))
      }
  }

  def updateUser(userId: Int, newRecord: UserInfo): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val count = SQL("UPDATE users SET name={name},email={email},phone={phone} WHERE survey_id={survey_id} AND user_id={user_id}")
          .on('user_id -> userId, 'name -> newRecord.name, 'email -> newRecord.email, 'phone -> newRecord.phone).executeUpdate()

        if (count == 1) {
          for (_ <- updateUserRolesById(Seq(RolesForId(userId, newRecord.roles))).right;
               _ <- updateCustomDataById(Seq(CustomDataForId(userId, newRecord.customFields))).right)
            yield ()
        }
        else
          Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
      }
  }

  private case class UserInfoRow(id: Int, name: Option[String], email: Option[String], phone: Option[String])

  private object UserInfoRow {
    val parser = Macro.namedParser[UserInfoRow]
  }

  private case class PasswordRow(password_hash: String, password_salt: String, password_hasher: String)

  private object PasswordRow {
    val parser = Macro.namedParser[PasswordRow]
  }


  def getUserById(userId: Int): Either[LookupError, UserInfoWithId] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone FROM users WHERE id={user_id}")
          .on('user_id -> userId)
          .as(UserInfoRow.parser.singleOpt) match {
          case Some(row) =>
            val roles = SQL("SELECT role FROM user_roles WHERE user_id={user_id}").on('user_id -> userId).as(SqlParser.str("role").*).toSet
            val custom_fields = SQL("SELECT name, value FROM user_custom_fields WHERE user_id={user_id}").on('user_id -> userId).as((SqlParser.str("name") ~ SqlParser.str("value")).*).map {
              case name ~ value => (name, value)
            }.toMap

            Right(UserInfoWithId(row.id, row.name, row.email, row.phone, roles, custom_fields))
          case None => Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
        }
      }
  }

  def getUserByAlias(alias: SurveyUserAlias): Either[LookupError, UserInfoWithId] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone FROM users JOIN user_survey_aliases ON users.id = user_survey_aliases.user_id WHERE survey_id={survey_id} AND user_name={user_name}")
          .on('survey_id -> alias.surveyId, 'user_name -> alias.userName)
          .as(UserInfoRow.parser.singleOpt) match {
          case Some(row) =>
            val roles = SQL("SELECT role FROM user_roles JOIN user_survey_aliases ON user_roles.user_id=user_survey_aliases.user_id WHERE survey_id={survey_id} AND user_name={user_name}")
              .on('survey_id -> alias.surveyId, 'user_name -> alias.userName)
              .as(SqlParser.str("role").*).toSet

            val custom_fields = SQL(
              """SELECT name, value FROM user_custom_fields JOIN user_survey_aliases ON user_custom_fields.user_id=user_survey_aliases.user_id
                |WHERE survey_id={survey_id} AND user_name={user_name}""".stripMargin)
              .on('survey_id -> alias.surveyId, 'user_name -> alias.userName)
              .as((SqlParser.str("name") ~ SqlParser.str("value")).*).map {
              case name ~ value => (name, value)
            }.toMap

            Right(UserInfoWithId(row.id, row.name, row.email, row.phone, roles, custom_fields))
          case None => Left(RecordNotFound(new RuntimeException(s"User alias ${alias.surveyId}/${alias.userName} does not exist")))
        }
      }
  }

  def deleteUsersById(userIds: Seq[Int]): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM users WHERE id IN({user_ids})").on('user_ids -> userIds).execute()

      Right(())
  }

  def deleteUsersByAlias(aliases: Seq[SurveyUserAlias]): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>

      val params = aliases.map {
        a =>
          Seq[NamedParameter]('survey_id -> a.surveyId, 'user_name -> a.userName)
      }

      if (params.nonEmpty) {

        BatchSql("DELETE FROM users WHERE id IN (SELECT user_id FROM user_survey_aliases AS a WHERE a.survey_id={survey_id} AND a.user_name={user_name})", params.head, params.tail: _*).execute()
        Right(())
      }

      else Right(())
  }

  def getUserPasswordById(userId: Int): Either[LookupError, SecurePassword] = tryWithConnection {
    implicit conn =>
      SQL("SELECT password_hash, password_salt, password_hasher FROM users WHERE id={user_id}")
        .on('user_id -> userId)
        .as(PasswordRow.parser.singleOpt) match {
        case Some(row) => Right(SecurePassword(row.password_hash, row.password_salt, row.password_hasher))
        case None => Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
      }
  }

  def getUserPasswordByAlias(alias: SurveyUserAlias): Either[LookupError, SecurePassword] = tryWithConnection {
    implicit conn =>
      SQL("SELECT password_hash, password_salt, password_hasher FROM users JOIN user_survey_aliases AS a WHERE a.survey_id={survey_id} AND a.user_name={user_name}")
        .on('survey_id -> alias.surveyId, 'user_name -> alias.userName)
        .as(PasswordRow.parser.singleOpt) match {
        case Some(row) => Right(SecurePassword(row.password_hash, row.password_salt, row.password_hasher))
        case None => Left(RecordNotFound(new RuntimeException(s"User alias ${alias.surveyId}/${alias.userName} does not exist")))
      }
  }

  def updateUserPassword(userId: Int, update: SecurePassword): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      val count = SQL("UPDATE users SET password_hash={hash},password_salt={salt},password_hasher={hasher} WHERE id={user_id}")
        .on('user_id -> userId, 'hash -> update.hashBase64, 'salt -> update.saltBase64, 'hasher -> update.hasher)
        .executeUpdate()

      if (count != 1)
        Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
      else
        Right(())
  }

  private def getUserRoles(userIds: Seq[Int])(implicit connection: java.sql.Connection): Map[Int, Set[String]] = {
    SQL("SELECT user_id, role FROM user_roles WHERE user_id IN {user_ids}")
      .on('user_ids -> userIds).as((SqlParser.int("user_id") ~ SqlParser.str("role")).*).foldLeft(Map[Int, Set[String]]()) {
      case (acc, userId ~ role) => acc + (userId -> (acc.getOrElse(userId, Set()) + role))
    }
  }

  private def getUserCustomData(userIds: Seq[Int])(implicit connection: java.sql.Connection): Map[Int, Map[String, String]] = {
    SQL("SELECT user_id, name, value FROM user_custom_fields WHERE user_id IN {user_ids}")
      .on('user_ids -> userIds).as((SqlParser.int("user_id") ~ SqlParser.str("name") ~ SqlParser.str("value")).*).foldLeft(Map[Int, Map[String, String]]()) {
      case (acc, userId ~ name ~ value) => acc + (userId -> (acc.getOrElse(userId, Map()) + (name -> value)))
    }
  }

  private def buildUserRecordsFromRows(rows: Seq[UserInfoRow])(implicit connection: java.sql.Connection): Seq[UserInfoWithId] = {
    val roles = getUserRoles(rows.map(_.id)).withDefaultValue(Set[String]())
    val customData = getUserCustomData(rows.map(_.id)).withDefaultValue(Map[String, String]())

    rows.map {
      row =>
        UserInfoWithId(row.id, row.name, row.email, row.phone, roles(row.id), customData(row.id))
    }
  }

  def findUsers(query: String, limit: Int): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val rows = SQL("SELECT id,name,email,phone FROM users WHERE name LIKE %{query}% ORDER BY id LIMIT {limit}")
          .on('query -> query, 'limit -> limit)
          .as(UserInfoRow.parser.*)
        Right(buildUserRecordsFromRows(rows))
      }
  }

  def listUsersByRole(role: String, offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val rows = SQL("SELECT id,name,email,phone FROM users JOIN user_roles ON users.id=user_roles.id AND role={role} OFFSET {offset} LIMIT {limit}")
          .on('role -> role, 'offset -> offset, 'limit -> limit)
          .as(UserInfoRow.parser.*)
        Right(buildUserRecordsFromRows(rows))
      }
  }

  private case class UserDataRow(name: String, value: String)

  def getCustomUserData(userId: Int): Either[LookupError, Map[String, String]] = tryWithConnection {
    implicit conn =>

      withTransaction {
        val userExists = SQL("SELECT 1 FROM users WHERE id={user_id}").on('user_id -> userId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (!userExists)
          Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
        else {

          val result = SQL("SELECT name, value FROM user_custom_fields WHERE user_id={user_id}")
            .on('user_id -> userId)
            .executeQuery().as(Macro.namedParser[UserDataRow].*).map(row => (row.name, row.value)).toMap
          Right(result)
        }
      }
  }

  def updateCustomUserData(userId: Int, data: Map[String, String]): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val userExists = SQL("SELECT 1 FROM users WHERE id={user_id}").on('user_id -> userId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (!userExists)
          Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
        else
          updateCustomDataById(Seq(CustomDataForId(userId, data)))
      }
  }

  def nextGeneratedUserId(surveyId: String): Either[UnexpectedDatabaseError, Int] = tryWithConnection {
    implicit conn =>
      Right(SQL("INSERT INTO gen_user_counters VALUES('demo', 0) ON CONFLICT(survey_id) DO UPDATE SET count=gen_user_counters.count+1 RETURNING count")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(SqlParser.int("count").single))
  }

  def getSurveySupportUsers(surveyId: String): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]] = tryWithConnection {
    implicit conn =>
      val rows = SQL("SELECT id,name,email,phone FROM users JOIN survey_support_staff ON id=survey_support_staff.user_id WHERE survey_id={survey_id}")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(UserInfoRow.parser.*)

      Right(buildUserRecordsFromRows(rows))
  }

  def getGlobalSupportUsers(): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]] = tryWithConnection {
    implicit conn =>
      val rows = SQL("SELECT id,name,email,phone FROM users JOIN global_support_staff ON id=global_support_staff.user_id")
        .executeQuery()
        .as(UserInfoRow.parser.*)

      Right(buildUserRecordsFromRows(rows))
  }

}