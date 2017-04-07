package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.sql.Connection
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import org.apache.commons.lang3.StringUtils
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class UserAdminImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends UserAdminService with SqlDataService with SqlResourceLoader {

  private case class RolesForId(userId: Long, roles: Set[String])

  private def updateUserRolesById(roles: Seq[RolesForId])(implicit connection: Connection): Either[RecordNotFound, Unit] = {
    if (roles.nonEmpty) {

      SQL("DELETE FROM user_roles WHERE user_id IN ({user_ids})")
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
      } else
        Right(())
    } else
      Right(())
  }

  private case class RolesForAlias(surveyId: String, userName: String, roles: Set[String])

  private def updateUserRolesByAlias(roles: Seq[RolesForAlias])(implicit connection: Connection): Either[RecordNotFound, Unit] = {
    if (roles.nonEmpty) {

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
          BatchSql("INSERT INTO user_roles (survey_id, user_id, role) SELECT {survey_id}, user_id, {role} FROM user_survey_aliases WHERE survey_id={survey_id} AND user_name={user_name}", params.head, params.tail: _*).execute()
          Right(())
        }
      }

      Right(())
    } else
      Right(())
  }

  private case class CustomDataForId(userId: Long, customData: Map[String, String])

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
            case (name, value) => Seq[NamedParameter]('user_id -> r.surveyId, 'user_name -> r.userName, 'name -> name, 'value -> value)
          }
      }

      if (!userCustomFieldParams.isEmpty)
        tryWithConstraintCheck("user_custom_fields_user_id_fkey", e => RecordNotFound(new RuntimeException(s"Could not update custom user data because one of the user records was not found"))) {
          BatchSql("INSERT INTO user_custom_fields VALUES ({user_id}, {name}, {value})", userCustomFieldParams.head, userCustomFieldParams.tail: _*).execute()
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
              'name -> u.userInfo.name, 'email -> u.userInfo.email, 'phone -> u.userInfo.phone, 'simple_name -> u.userInfo.name.map(StringUtils.stripAccents(_).toLowerCase()))
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

  def createUser(newUser: NewUser): Either[CreateError, Long] = tryWithConnection {
    implicit conn =>
      withTransaction {

        tryWithConstraintCheck[CreateError, Long]("users_email_unique", e => DuplicateCode(e)) {

          val userId = SQL("INSERT INTO users VALUES (DEFAULT, {password_hash}, {password_salt}, {password_hasher}, {name}, {email}, {phone}, {simple_name})")
            .on('password_hash -> newUser.password.hashBase64,
              'password_salt -> newUser.password.saltBase64,
              'password_hasher -> newUser.password.hasher,
              'simple_name -> newUser.userInfo.name.map(StringUtils.stripAccents(_).toLowerCase()),
              'name -> newUser.userInfo.name,
              'email -> newUser.userInfo.email,
              'phone -> newUser.userInfo.phone)
            .executeInsert(SqlParser.scalar[Long].single)

          (for (
            _ <- updateUserRolesById(Seq(RolesForId(userId, newUser.userInfo.roles))).right;
            _ <- updateCustomDataById(Seq(CustomDataForId(userId, newUser.userInfo.customFields))).right
          ) yield userId).left.map(e => UnexpectedDatabaseError(e.exception))
        }
      }
  }

  def updateUser(userId: Long, newRecord: UserInfo): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val count = SQL("UPDATE users SET name={name},email={email},phone={phone},simple_name={simple_name} WHERE survey_id={survey_id} AND user_id={user_id}")
          .on('user_id -> userId, 'name -> newRecord.name, 'email -> newRecord.email, 'phone -> newRecord.phone,
            'simple_name -> newRecord.name.map(StringUtils.stripAccents(_).toLowerCase())).executeUpdate()

        if (count == 1) {
          for (_ <- updateUserRolesById(Seq(RolesForId(userId, newRecord.roles))).right;
               _ <- updateCustomDataById(Seq(CustomDataForId(userId, newRecord.customFields))).right)
            yield ()
        }
        else
          Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
      }
  }

  private case class UserInfoRow(id: Long, name: Option[String], email: Option[String], phone: Option[String])

  private object UserInfoRow {
    val parser = Macro.namedParser[UserInfoRow]
  }

  private case class PasswordRow(password_hash: String, password_salt: String, password_hasher: String)

  private object PasswordRow {
    val parser = Macro.namedParser[PasswordRow]
  }


  def getUserById(userId: Long): Either[LookupError, UserInfoWithId] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone FROM users WHERE id={user_id}")
          .on('user_id -> userId)
          .as(UserInfoRow.parser.singleOpt) match {
          case Some(row) => Right(buildUserRecordsFromRows(Seq(row)).head)
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
          case Some(row) => Right(buildUserRecordsFromRows(Seq(row)).head)
          case None => Left(RecordNotFound(new RuntimeException(s"User alias ${alias.surveyId}/${alias.userName} does not exist")))
        }
      }
  }

  def getUserByEmail(email: String): Either[LookupError, UserInfoWithId] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone FROM users WHERE email={email}")
          .on('email -> email)
          .as(UserInfoRow.parser.singleOpt) match {
          case Some(row) => Right(buildUserRecordsFromRows(Seq(row)).head)
          case None => Left(RecordNotFound(new RuntimeException(s"User with e-mail address <$email> does not exist")))
        }
      }
  }

  def deleteUsersById(userIds: Seq[Long]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      SQL("DELETE FROM users WHERE id IN({user_ids})").on('user_ids -> userIds).execute()

      Right(())
  }

  def deleteUsersByAlias(aliases: Seq[SurveyUserAlias]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
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

  def getUserPasswordById(userId: Long): Either[LookupError, SecurePassword] = tryWithConnection {
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
      SQL("SELECT password_hash, password_salt, password_hasher FROM users JOIN user_survey_aliases AS a ON a.user_id=users.id WHERE a.survey_id={survey_id} AND a.user_name={user_name}")
        .on('survey_id -> alias.surveyId, 'user_name -> alias.userName)
        .as(PasswordRow.parser.singleOpt) match {
        case Some(row) => Right(SecurePassword(row.password_hash, row.password_salt, row.password_hasher))
        case None => Left(RecordNotFound(new RuntimeException(s"User alias ${alias.surveyId}/${alias.userName} does not exist")))
      }
  }

  def getUserPasswordByEmail(email: String): Either[LookupError, SecurePassword] = tryWithConnection {
    implicit conn =>
      SQL("SELECT password_hash, password_salt, password_hasher FROM users WHERE email={email}")
        .on('email -> email)
        .as(PasswordRow.parser.singleOpt) match {
        case Some(row) => Right(SecurePassword(row.password_hash, row.password_salt, row.password_hasher))
        case None => Left(RecordNotFound(new RuntimeException(s"User with e-mail addess <$email> does not exist")))
      }
  }

  def updateUserPassword(userId: Long, update: SecurePassword): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      val count = SQL("UPDATE users SET password_hash={hash},password_salt={salt},password_hasher={hasher} WHERE id={user_id}")
        .on('user_id -> userId, 'hash -> update.hashBase64, 'salt -> update.saltBase64, 'hasher -> update.hasher)
        .executeUpdate()

      if (count != 1)
        Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
      else
        Right(())
  }

  private def getUserRoles(userIds: Seq[Long])(implicit connection: java.sql.Connection): Map[Long, Set[String]] = {
    if (userIds.isEmpty)
      Map()
    else
      SQL("SELECT user_id, role FROM user_roles WHERE user_id IN ({user_ids})")
        .on('user_ids -> userIds).as((SqlParser.long("user_id") ~ SqlParser.str("role")).*).foldLeft(Map[Long, Set[String]]()) {
        case (acc, userId ~ role) => acc + (userId -> (acc.getOrElse(userId, Set()) + role))
      }
  }

  private def getUserCustomData(userIds: Seq[Long])(implicit connection: java.sql.Connection): Map[Long, Map[String, String]] = {
    if (userIds.isEmpty)
      Map()
    else
      SQL("SELECT user_id, name, value FROM user_custom_fields WHERE user_id IN ({user_ids})")
        .on('user_ids -> userIds).as((SqlParser.long("user_id") ~ SqlParser.str("name") ~ SqlParser.str("value")).*).foldLeft(Map[Long, Map[String, String]]()) {
        case (acc, userId ~ name ~ value) => acc + (userId -> (acc.getOrElse(userId, Map()) + (name -> value)))
      }
  }

  private def getUserAliases(userIds: Seq[Long])(implicit connection: java.sql.Connection): Map[Long, Set[String]] = {
    if (userIds.isEmpty)
      Map()
    else
      SQL("SELECT user_id, user_name FROM user_survey_aliases WHERE user_id IN ({user_ids})")
        .on('user_ids -> userIds).as((SqlParser.long("user_id") ~ SqlParser.str("user_name")).*).foldLeft(Map[Long, Set[String]]()) {
        case (acc, userId ~ userName) => acc + (userId -> (acc.getOrElse(userId, Set()) + userName))
      }
  }

  private def buildUserRecordsFromRows(rows: Seq[UserInfoRow])(implicit connection: java.sql.Connection): Seq[UserInfoWithId] = {
    val roles = getUserRoles(rows.map(_.id)).withDefaultValue(Set[String]())
    val customData = getUserCustomData(rows.map(_.id)).withDefaultValue(Map[String, String]())
    val userAliases = getUserAliases(rows.map(_.id)).withDefaultValue(Set[String]())

    rows.map {
      row =>
        UserInfoWithId(row.id, row.name, row.email, row.phone, roles(row.id), userAliases(row.id), customData(row.id))
    }
  }

  def findUsers(query: String, limit: Int): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val rows = SQL("SELECT id,name,email,phone FROM users WHERE (simple_name LIKE {name_query} OR email LIKE {query}) ORDER BY id LIMIT {limit}")
          .on('name_query -> ("%" + AnormUtil.escapeLike(StringUtils.stripAccents(query).toLowerCase()) + "%"), 'query -> ("%" + AnormUtil.escapeLike(query) + "%"), 'limit -> limit)
          .as(UserInfoRow.parser.*)
        Right(buildUserRecordsFromRows(rows))
      }
  }

  def listUsersByRole(role: String, offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val rows = SQL("SELECT id,name,email,phone FROM users JOIN user_roles ON users.id=user_roles.user_id AND role={role} OFFSET {offset} LIMIT {limit}")
          .on('role -> role, 'offset -> offset, 'limit -> limit)
          .as(UserInfoRow.parser.*)
        Right(buildUserRecordsFromRows(rows))
      }
  }

  private case class UserDataRow(name: String, value: String)

  def getCustomUserData(userId: Long): Either[LookupError, Map[String, String]] = tryWithConnection {
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

  def updateCustomUserData(userId: Long, data: Map[String, String]): Either[UpdateError, Unit] = tryWithConnection {
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