package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.sql.Connection
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import org.apache.commons.lang3.StringUtils
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class UserAdminImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends UserAdminService with SqlDataService with SqlResourceLoader {

  private case class RolesForId(userId: Long, roles: Set[String])

  private case class SecurePasswordForId(userId: Long, password: SecurePassword)

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
          BatchSql("INSERT INTO user_roles(user_id, role) SELECT user_id,{role} FROM user_survey_aliases WHERE survey_id={survey_id} AND user_name={user_name}", params.head, params.tail: _*).execute()
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
            case (name, value) => Seq[NamedParameter]('survey_id -> r.surveyId, 'user_name -> r.userName, 'name -> name, 'value -> value)
          }
      }

      if (!userCustomFieldParams.isEmpty)
        tryWithConstraintCheck("user_custom_fields_user_id_fkey", e => RecordNotFound(new RuntimeException(s"Could not update custom user data because one of the user records was not found"))) {
          BatchSql("INSERT INTO user_custom_fields (user_id, name, value) SELECT user_id, {name}, {value} FROM user_survey_aliases WHERE user_name={user_name} AND survey_id={survey_id}", userCustomFieldParams.head, userCustomFieldParams.tail: _*).execute()
          Right(())
        }
      else
        Right(())
    } else
      Right(())
  }

  private case class SecurePasswordForAlias(alias: SurveyUserAlias, password: SecurePassword)

  private def createOrUpdatePasswordsByAlias(passwords: Seq[SecurePasswordForAlias])(implicit connection: Connection): Either[UnexpectedDatabaseError, Unit] = {
    if (passwords.nonEmpty) {
      val params = passwords.map {
        case p =>
          Seq[NamedParameter]('user_name -> p.alias.userName, 'survey_id -> p.alias.surveyId, 'password_hash -> p.password.hashBase64, 'password_salt -> p.password.saltBase64,
            'password_hasher -> p.password.hasher)
      }

      BatchSql(
        """INSERT INTO user_passwords(user_id, password_hash, password_salt, password_hasher)
          |  SELECT user_id,{password_hash},{password_salt},{password_hasher} FROM user_survey_aliases WHERE user_name={user_name} AND survey_id={survey_id}
          |  ON CONFLICT(user_id) DO UPDATE SET password_hash=excluded.password_hash,password_salt=excluded.password_salt,password_hasher=excluded.password_hasher""".stripMargin,
        params.head, params.tail: _*).execute()

      Right()
    } else
      Right()
  }

  private lazy val createOrUpdateUserByAliasQuery = sqlFromResource("admin/users/create_or_update_user_by_alias.sql")

  def createOrUpdateUsersWithAliases(usersWithAliases: Seq[NewUserWithAlias]): Either[DependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val userUpsertParams = usersWithAliases.map {
          u =>
            Seq[NamedParameter]('survey_id -> u.alias.surveyId, 'user_name -> u.alias.userName, 'url_auth_token -> URLAuthTokenUtils.generateToken, 'name -> u.userInfo.name,
              'email -> u.userInfo.email, 'phone -> u.userInfo.phone, 'simple_name -> u.userInfo.name.map(StringUtils.stripAccents(_).toLowerCase()))
        }

        if (userUpsertParams.nonEmpty) {
          tryWithConstraintCheck[DependentUpdateError, Unit]("users_email_unique", e => DuplicateCode(e)) {
            BatchSql(createOrUpdateUserByAliasQuery, userUpsertParams.head, userUpsertParams.tail: _*).execute()

            for (
              _ <- updateUserRolesByAlias(usersWithAliases.foldLeft(List[RolesForAlias]()) {
                case (acc, userRecord) => RolesForAlias(userRecord.alias.surveyId, userRecord.alias.userName, userRecord.userInfo.roles) +: acc
              }).right;
              _ <- updateCustomDataByAlias(usersWithAliases.foldLeft(List[CustomDataForAlias]()) {
                case (acc, record) => CustomDataForAlias(record.alias.surveyId, record.alias.userName, record.userInfo.customFields) +: acc
              }).right;
              _ <- createOrUpdatePasswordsByAlias(usersWithAliases.map(u => SecurePasswordForAlias(u.alias, u.password))).right
            ) yield ()
          }
        }
        else
          Right(())
      }
  }

  def createUsersQuery(newUsers: Seq[NewUserProfile])(implicit connection: Connection): Either[CreateError, Seq[Long]] = {
    if (newUsers.isEmpty)
      Right(Seq())
    else {
      val userParams = newUsers.map {
        newUser =>
          Seq[NamedParameter]('simple_name -> newUser.name.map(StringUtils.stripAccents(_).toLowerCase()),
            'name -> newUser.name,
            'email -> newUser.email,
            'phone -> newUser.phone)
      }

      tryWithConstraintCheck[CreateError, Seq[Long]]("users_email_unique", e => DuplicateCode(e)) {

        val batchSql = BatchSql("INSERT INTO users(name,email,phone,simple_name) VALUES ({name},{email},{phone},{simple_name})", userParams.head, userParams.tail: _*)

        val userIds = AnormUtil.batchKeys(batchSql)

        val userInfoWithId = newUsers.zip(userIds)

        (for (
          _ <- updateUserRolesById(userInfoWithId.map { case (userInfo, userId) => RolesForId(userId, userInfo.roles) }).right;
          _ <- updateCustomDataById(userInfoWithId.map { case (userInfo, userId) => CustomDataForId(userId, userInfo.customFields) }).right
        ) yield userIds).left.map(e => UnexpectedDatabaseError(e.exception))
      }
    }
  }

  private def createPasswordsQuery(passwords: Seq[SecurePasswordForId])(implicit connection: Connection): Either[CreateError, Unit] = {
    if (passwords.isEmpty)
      Right(())
    else {
      val passwordParams = passwords.map {
        p =>
          Seq[NamedParameter]('user_id -> p.userId, 'hash -> p.password.hashBase64, 'salt -> p.password.saltBase64, 'hasher -> p.password.hasher)
      }

      BatchSql("INSERT INTO user_passwords(user_id, password_hash, password_salt, password_hasher) VALUES({user_id},{hash},{salt},{hasher})", passwordParams.head, passwordParams.tail: _*).execute()

      Right(())
    }
  }

  def createUserWithPassword(newUser: NewUserWithPassword): Either[CreateError, Long] = tryWithConnection {
    implicit conn =>
      for (userId <- createUsersQuery(Seq(newUser.userInfo)).right.map(_.head).right;
           _ <- createPasswordsQuery(Seq(SecurePasswordForId(userId, newUser.password))).right
      ) yield userId
  }

  def createUsers(newUsers: Seq[NewUserProfile]): Either[CreateError, Seq[Long]] = tryWithConnection {
    implicit conn =>
      createUsersQuery(newUsers)
  }

  def updateUser(userId: Long, update: UserProfileUpdate): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        tryWithConstraintCheck[UpdateError, Unit]("users_email_unique", e => DuplicateCode(e)) {
          val count = SQL("UPDATE users SET name={name},email={email},phone={phone},simple_name={simple_name},email_notifications={email_n},sms_notifications={sms_n} WHERE id={user_id}")
            .on('user_id -> userId, 'name -> update.name, 'email -> update.email, 'phone -> update.phone,
              'email_n -> update.emailNotifications, 'sms_n -> update.smsNotifications,
              'simple_name -> update.name.map(StringUtils.stripAccents(_).toLowerCase())).executeUpdate()

          if (count == 1) {
            for (_ <- updateUserRolesById(Seq(RolesForId(userId, update.roles))).right;
                 _ <- updateCustomDataById(Seq(CustomDataForId(userId, update.customFields))).right)
              yield ()
          }
          else
            Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
        }
      }
  }

  private case class UserInfoRow(id: Long, name: Option[String], email: Option[String], phone: Option[String], email_notifications: Boolean, sms_notifications: Boolean)

  private object UserProfileRow {
    val parser = Macro.namedParser[UserInfoRow]
  }

  private case class PasswordRow(password_hash: String, password_salt: String, password_hasher: String)

  private object PasswordRow {
    val parser = Macro.namedParser[PasswordRow]
  }

  def getUserById(userId: Long): Either[LookupError, UserProfile] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone, email_notifications, sms_notifications FROM users WHERE id={user_id}")
          .on('user_id -> userId)
          .as(UserProfileRow.parser.singleOpt) match {
          case Some(row) => Right(buildUserRecordsFromRows(Seq(row)).head)
          case None => Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
        }
      }
  }

  def getUserByAlias(alias: SurveyUserAlias): Either[LookupError, UserProfile] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone, email_notifications, sms_notifications FROM users JOIN user_survey_aliases ON users.id = user_survey_aliases.user_id WHERE survey_id={survey_id} AND user_name={user_name}")
          .on('survey_id -> alias.surveyId, 'user_name -> alias.userName)
          .as(UserProfileRow.parser.singleOpt) match {
          case Some(row) => Right(buildUserRecordsFromRows(Seq(row)).head)
          case None => Left(RecordNotFound(new RuntimeException(s"User alias ${alias.surveyId}/${alias.userName} does not exist")))
        }
      }
  }

  def validateUrlToken(token: String): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      val valid = SQL("SELECT 1 FROM user_survey_aliases WHERE url_auth_token={token}").on('token -> token).executeQuery().as(SqlParser.long(1).singleOpt).isDefined

      if (valid)
        Right(())
      else
        Left(RecordNotFound(new RuntimeException(s"Invalid URL authentication token: $token")))
  }

  def getUserByUrlToken(token: String): Either[LookupError, UserProfile] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone, email_notifications, sms_notifications FROM users JOIN user_survey_aliases ON users.id = user_survey_aliases.user_id WHERE user_survey_aliases.url_auth_token={token}")
          .on('token -> token)
          .as(UserProfileRow.parser.singleOpt) match {
          case Some(row) => Right(buildUserRecordsFromRows(Seq(row)).head)
          case None => Left(RecordNotFound(new RuntimeException(s"User with given URL authentication token not found")))
        }
      }
  }

  def getUserByEmail(email: String): Either[LookupError, UserProfile] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT id, name, email, phone, email_notifications, sms_notifications FROM users WHERE email={email}")
          .on('email -> email)
          .as(UserProfileRow.parser.singleOpt) match {
          case Some(row) => Right(buildUserRecordsFromRows(Seq(row)).head)
          case None => Left(RecordNotFound(new RuntimeException(s"User with e-mail address <$email> does not exist")))
        }
      }
  }

  def deleteUsersById(userIds: Seq[Long]): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>

      tryWithConstraintCheck("survey_submissions_user_id_fkey", e => StillReferenced(new RuntimeException("User cannot be deleted because they have survey submissions", e))) {
        SQL("DELETE FROM users WHERE id IN({user_ids})").on('user_ids -> userIds).execute()

        Right(())
      }
  }

  def deleteUsersByAlias(aliases: Seq[SurveyUserAlias]): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>

      tryWithConstraintCheck("survey_submissions_user_id_fkey", e => StillReferenced(new RuntimeException("User cannot be deleted because they have survey submissions", e))) {
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
  }

  def getUserPasswordById(userId: Long): Either[LookupError, SecurePassword] = tryWithConnection {
    implicit conn =>
      SQL("SELECT password_hash, password_salt, password_hasher FROM user_passwords WHERE user_id={user_id}")
        .on('user_id -> userId)
        .as(PasswordRow.parser.singleOpt) match {
        case Some(row) => Right(SecurePassword(row.password_hash, row.password_salt, row.password_hasher))
        case None => Left(RecordNotFound(new RuntimeException(s"User $userId does not exist")))
      }
  }

  def getUserPasswordByAlias(alias: SurveyUserAlias): Either[LookupError, SecurePassword] = tryWithConnection {
    implicit conn =>
      SQL("SELECT password_hash, password_salt, password_hasher FROM user_passwords JOIN user_survey_aliases AS a ON a.user_id=user_passwords.user_id WHERE a.survey_id={survey_id} AND a.user_name={user_name}")
        .on('survey_id -> alias.surveyId, 'user_name -> alias.userName)
        .as(PasswordRow.parser.singleOpt) match {
        case Some(row) => Right(SecurePassword(row.password_hash, row.password_salt, row.password_hasher))
        case None => Left(RecordNotFound(new RuntimeException(s"User alias ${alias.surveyId}/${alias.userName} does not exist")))
      }
  }

  def getUserPasswordByEmail(email: String): Either[LookupError, SecurePassword] = tryWithConnection {
    implicit conn =>
      SQL("SELECT password_hash, password_salt, password_hasher FROM user_passwords JOIN users ON user_passwords.user_id=users.id WHERE users.email={email}")
        .on('email -> email)
        .as(PasswordRow.parser.singleOpt) match {
        case Some(row) => Right(SecurePassword(row.password_hash, row.password_salt, row.password_hasher))
        case None => Left(RecordNotFound(new RuntimeException(s"User with e-mail addess <$email> does not exist")))
      }
  }

  def updateUserPassword(userId: Long, update: SecurePassword): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      val count = SQL("UPDATE user_passwords SET password_hash={hash},password_salt={salt},password_hasher={hasher} WHERE user_id={user_id}")
        .on('user_id -> userId, 'hash -> update.hashBase64, 'salt -> update.saltBase64, 'hasher -> update.hasher)
        .executeUpdate()

      if (count != 1)
        Left(RecordNotFound(new RuntimeException(s"Password for $userId does not exist")))
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

  def createSurveyUserAliasesQuery(surveyId: String, surveyUserNames: Map[Long, String])(implicit connection: java.sql.Connection): Either[DependentCreateError, Map[Long, String]] = {
    if (surveyUserNames.isEmpty)
      Right(Map())
    else {
      val errors = Map[String, PSQLException => DependentCreateError](
        "user_aliases_pkey" -> (e => DuplicateCode(e)),
        "user_aliases_user_id_fkey" -> (e => ParentRecordNotFound(e)),
        "user_aliases_survey_id_fkey" -> (e => ParentRecordNotFound(e))
      )

      val authTokens = surveyUserNames.map {
        case (userId, _) => (userId, URLAuthTokenUtils.generateToken)
      }

      val params = surveyUserNames.toSeq.map {
        case (userId, userName) =>
          Seq[NamedParameter]('user_id -> userId, 'survey_id -> surveyId, 'user_name -> userName, 'auth_token -> authTokens(userId))
      }

      tryWithConstraintsCheck[DependentCreateError, Map[Long, String]](errors) {
        BatchSql("INSERT INTO user_survey_aliases (user_id, survey_id, user_name, url_auth_token) VALUES ({user_id},{survey_id},{user_name},{auth_token})", params.head, params.tail: _*).execute()
        Right(authTokens)
      }
    }
  }

  def getSurveyUserAliases(userIds: Seq[Long], surveyId: String): Either[UnexpectedDatabaseError, Map[Long, String]] = tryWithConnection {
    implicit connection =>
      if (userIds.isEmpty)
        Right(Map())
      else
        Right(SQL("SELECT user_id, user_name FROM user_survey_aliases WHERE user_id IN ({user_ids}) AND survey_id={survey_id}")
          .on('user_ids -> userIds, 'survey_id -> surveyId)
          .as((SqlParser.long("user_id") ~ SqlParser.str("user_name")).*).foldLeft(Map[Long, String]()) {
          case (acc, userId ~ userName) => acc + (userId -> userName)
        })
  }

  private def buildUserRecordsFromRows(rows: Seq[UserInfoRow])(implicit connection: java.sql.Connection): Seq[UserProfile] = {
    val roles = getUserRoles(rows.map(_.id)).withDefaultValue(Set[String]())
    val customData = getUserCustomData(rows.map(_.id)).withDefaultValue(Map[String, String]())

    rows.map {
      row =>
        UserProfile(row.id, row.name, row.email, row.phone, row.email_notifications, row.sms_notifications, roles(row.id), customData(row.id))
    }
  }

  def findUsers(query: String, limit: Int): Either[UnexpectedDatabaseError, Seq[UserProfile]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val rows = SQL("SELECT id,name,email,phone,email_notifications,sms_notifications FROM users WHERE (simple_name LIKE {name_query} OR email LIKE {query}) ORDER BY id LIMIT {limit}")
          .on('name_query -> ("%" + AnormUtil.escapeLike(StringUtils.stripAccents(query).toLowerCase()) + "%"), 'query -> ("%" + AnormUtil.escapeLike(query) + "%"), 'limit -> limit)
          .as(UserProfileRow.parser.*)
        Right(buildUserRecordsFromRows(rows))
      }
  }

  def listUsersByRole(role: String, offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[UserProfile]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val rows = SQL("SELECT id,name,email,phone,email_notifications,sms_notifications FROM users JOIN user_roles ON users.id=user_roles.user_id AND role={role} OFFSET {offset} LIMIT {limit}")
          .on('role -> role, 'offset -> offset, 'limit -> limit)
          .as(UserProfileRow.parser.*)
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

}