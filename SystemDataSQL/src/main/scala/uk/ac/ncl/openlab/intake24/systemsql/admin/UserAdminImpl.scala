package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.sql.Connection
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

import scala.annotation.tailrec

class UserAdminImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends UserAdminService with SqlDataService with SqlResourceLoader {

  private def updateUserRolesQuery(surveyId: Option[String], roles: Map[String, Set[String]])(implicit connection: Connection): Either[ParentError, Unit] = {
    SQL("DELETE FROM user_roles WHERE survey_id={survey_id} AND user_id IN ({user_ids})")
      .on('survey_id -> surveyId.getOrElse(""), 'user_ids -> roles.keySet.toSeq)
      .execute()

    val roleParams = roles.toSeq.flatMap {
      case (userName, roles) =>
        roles.map {
          role =>
            Seq[NamedParameter]('survey_id -> surveyId.getOrElse(""), 'user_id -> userName, 'role -> role)
        }
    }

    if (!roleParams.isEmpty) {
      tryWithConstraintCheck("user_roles_users_fk", e => ParentRecordNotFound(new RuntimeException(s"Could not update roles because one of the user records was not found"))) {
        BatchSql("INSERT INTO user_roles VALUES (DEFAULT, {survey_id}, {user_id}, {role})", roleParams.head, roleParams.tail: _*).execute()
        Right(())
      }
    } else
      Right(())
  }

  private def updateUserPermissionsQuery(surveyId: Option[String], permissions: Map[String, Set[String]])(implicit connection: Connection): Either[ParentError, Unit] = {
    SQL("DELETE FROM user_permissions WHERE survey_id={survey_id} AND user_id IN ({user_ids})")
      .on('survey_id -> surveyId.getOrElse(""), 'user_ids -> permissions.keySet.toSeq)
      .execute()

    val permissionParams = permissions.toSeq.flatMap {
      case (userName, permissions) =>
        permissions.map {
          permission =>
            Seq[NamedParameter]('survey_id -> surveyId.getOrElse(""), 'user_id -> userName, 'permission -> permission)
        }
    }

    if (!permissionParams.isEmpty) {
      tryWithConstraintCheck("user_permissions_users_fk", e => ParentRecordNotFound(new RuntimeException(s"Could not update permissions because one of the user records was not found"))) {
        BatchSql("INSERT INTO user_permissions VALUES (DEFAULT, {survey_id}, {user_id}, {permission})", permissionParams.head, permissionParams.tail: _*).execute()
        Right(())
      }
    } else
      Right(())
  }

  private def updateUserCustomDataQuery(surveyId: Option[String], customData: Map[String, Map[String, String]])(implicit connection: Connection): Either[ParentError, Unit] = {
    SQL("DELETE FROM user_custom_fields WHERE survey_id={survey_id} AND user_id IN ({user_ids})")
      .on('survey_id -> surveyId.getOrElse(""), 'user_ids -> customData.keySet.toSeq)
      .execute()

    val userCustomFieldParams = customData.toSeq.flatMap {
      case (userName, data) =>
        data.map {
          case (name, value) => Seq[NamedParameter]('survey_id -> surveyId.getOrElse(""), 'user_id -> userName, 'name -> name, 'value -> value)
        }
    }

    if (!userCustomFieldParams.isEmpty)
      tryWithConstraintCheck("user_custom_fields_users_fk", e => ParentRecordNotFound(new RuntimeException(s"Could not update custom user data because one of the user records was not found"))) {
        BatchSql("INSERT INTO user_custom_fields VALUES (DEFAULT, {survey_id}, {user_id}, {name}, {value})", userCustomFieldParams.head, userCustomFieldParams.tail: _*).execute()
        Right(())
      }
    else
      Right(())
  }

  def createOrUpdateUsers(surveyId: Option[String], userRecords: Seq[SecureUserRecord]): Either[DependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val userParams = userRecords.map {
          record =>
            Seq[NamedParameter]('id -> record.userName, 'survey_id -> surveyId.getOrElse(""), 'password_hash -> record.passwordHashBase64, 'password_salt -> record.passwordSaltBase64,
              'password_hasher -> record.passwordHasher, 'name -> record.name, 'email -> record.email, 'phone -> record.phone)
        }

        if (userParams.nonEmpty) {
          BatchSql("INSERT INTO users VALUES ({id}, {survey_id}, {password_hash}, {password_salt}, {password_hasher}, {name}, {email}, {phone}) " +
            "ON CONFLICT ON CONSTRAINT users_id_pk DO UPDATE " +
            "SET password_hash={password_hash},password_salt={password_salt},password_hasher={password_hasher},name={name},email={email},phone={phone}", userParams.head, userParams.tail: _*).execute()

          for (
            _ <- updateUserRolesQuery(surveyId, userRecords.foldLeft(Map[String, Set[String]]()) {
              case (acc, record) => acc + (record.userName -> record.roles)
            }).right;
            _ <- updateUserPermissionsQuery(surveyId, userRecords.foldLeft(Map[String, Set[String]]()) {
              case (acc, record) => acc + (record.userName -> record.permissions)
            }).right;
            _ <- updateUserCustomDataQuery(surveyId, userRecords.foldLeft(Map[String, Map[String, String]]()) {
              case (acc, record) => acc + (record.userName -> record.customFields)
            }).right
          ) yield ()
        } else
          Right(())
      }
  }

  def createUser(surveyId: Option[String], userRecord: SecureUserRecord): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        tryWithConstraintCheck[DependentCreateError, Unit]("users_id_pk", e => DuplicateCode(new RuntimeException(s"User name ${userRecord.userName} already exists for survey $surveyId"))) {
          SQL("INSERT INTO users VALUES ({id}, {survey_id}, {password_hash}, {password_salt}, {password_hasher}, {name}, {email}, {phone})")
            .on(
              'id -> userRecord.userName,
              'survey_id -> surveyId.getOrElse(""),
              'password_hash -> userRecord.passwordHashBase64,
              'password_salt -> userRecord.passwordSaltBase64,
              'password_hasher -> userRecord.passwordHasher,
              'name -> userRecord.name,
              'email -> userRecord.email,
              'phone -> userRecord.phone)
            .execute()
          for (
            _ <- updateUserRolesQuery(surveyId, Map(userRecord.userName -> userRecord.roles)).right;
            _ <- updateUserPermissionsQuery(surveyId, Map(userRecord.userName -> userRecord.permissions)).right;
            _ <- updateUserCustomDataQuery(surveyId, Map(userRecord.userName -> userRecord.customFields)).right
          ) yield ()
        }
      }
  }

  private case class UserDataRow(name: String, value: String)

  def getCustomUserData(surveyId: Option[String], userId: String): Either[LookupError, Map[String, String]] = tryWithConnection {
    implicit conn =>

      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId.getOrElse("")).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty
        val userExists = SQL("SELECT 1 FROM users WHERE id={user_id} AND survey_id={survey_id}").on('user_id -> userId, 'survey_id -> surveyId.getOrElse("")).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (!surveyExists)
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
        else if (!userExists)
          Left(RecordNotFound(new RuntimeException(s"User $userId does not exist in survey $surveyId")))
        else {

          val result = SQL("SELECT name, value FROM user_custom_fields WHERE (user_id={user_id} AND survey_id={survey_id})")
            .on('survey_id -> surveyId.getOrElse(""), 'user_id -> userId)
            .executeQuery().as(Macro.namedParser[UserDataRow].*).map(row => (row.name, row.value)).toMap
          Right(result)
        }
      }
  }

  def updateCustomUserData(surveyId: Option[String], userId: String, data: Map[String, String]): Either[ParentError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId.getOrElse("")).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty
        val userExists = SQL("SELECT 1 FROM users WHERE id={user_id} AND survey_id={survey_id}").on('user_id -> userId, 'survey_id -> surveyId.getOrElse("")).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (!surveyExists)
          Left(ParentRecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
        else if (!userExists)
          Left(ParentRecordNotFound(new RuntimeException(s"User $userId does not exist in survey $surveyId")))
        else {
          updateUserCustomDataQuery(surveyId, Map(userId -> data))
        }
      }
  }

  private case class ShortUserRecordRow(password_hash: String, password_salt: String, password_hasher: String, name: Option[String], email: Option[String], phone: Option[String])

  def getUserById(surveyId: Option[String], userId: String): Either[LookupError, SecureUserRecord] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT password_hash, password_salt, password_hasher, name, email, phone FROM users WHERE (survey_id={survey_id} AND id={user_id})")
          .on('survey_id -> surveyId.getOrElse(""), 'user_id -> userId).executeQuery()
          .as(Macro.namedParser[ShortUserRecordRow].singleOpt) match {
          case Some(row) =>
            val roles = SQL("SELECT role FROM user_roles WHERE (survey_id={survey_id} AND user_id={user_id})").on('survey_id -> surveyId, 'user_id -> userId).as(SqlParser.str("role").*)
            val permissions = SQL("SELECT permission FROM user_permissions WHERE (survey_id={survey_id} AND user_id={user_id})").on('survey_id -> surveyId, 'user_id -> userId).as(SqlParser.str("permission").*)
            val custom_fields = SQL("SELECT name, value FROM user_custom_fields WHERE (user_id={user_id} AND survey_id={survey_id})").on('survey_id -> surveyId, 'user_id -> userId).as((SqlParser.str("name") ~ SqlParser.str("value")).*).map {
              case name ~ value => (name, value)
            }.toMap

            Right(SecureUserRecord(userId, row.password_hash, row.password_salt, row.password_hasher, row.name, row.email, row.phone, roles.toSet, permissions.toSet, custom_fields))
          case None => Left(RecordNotFound(new RuntimeException(s"User $userId does not exist in survey $surveyId")))
        }
      }
  }

  private case class UserRecordRow(survey_id: String, user_id: String, password_hash: String, password_salt: String, password_hasher: String, name: Option[String], email: Option[String], phone: Option[String])

  private case class RoleRecordRow(survey_id: String, user_id: String, role: String)

  private case class PermissionRecordRow(survey_id: String, user_id: String, permission: String)

  private case class CustomFieldRecordRow(survey_id: String, user_id: String, name: String, value: String)

  private case class PublicUserRecordWithPermissionsRow(survey_id: Option[String], user_id: String, name: Option[String], email: Option[String], phone: Option[String], custom_fields: Array[Array[String]], roles: Array[String], permissions: Array[String])

  private case class PublicUserRecordRow(survey_id: Option[String], user_id: String, name: Option[String], email: Option[String], phone: Option[String], custom_fields: Array[Array[String]]) {
    def toPublicUserRecord = {
      val customFields = custom_fields.foldLeft(Map[String, String]()) {
        case (result, Array(name, value)) => result + (name -> value)
      }

      PublicUserRecord(survey_id, user_id, name, email, phone, customFields)
    }
  }

  @tailrec
  private def buildUserRecords(
                                userRows: List[UserRecordRow],
                                roleRows: List[RoleRecordRow],
                                permRows: List[PermissionRecordRow],
                                customFieldRows: List[CustomFieldRecordRow],
                                result: List[SecureUserRecord] = List()): List[SecureUserRecord] = userRows match {
    case Nil => result.reverse
    case curUserRow :: restOfUserRows => {
      val (curUserRoleRows, restOfRoleRows) = roleRows.span(r => (r.survey_id == curUserRow.survey_id && r.user_id == curUserRow.user_id))
      val (curUserPermRows, restOfPermRows) = permRows.span(r => (r.survey_id == curUserRow.survey_id && r.user_id == curUserRow.user_id))
      val (curUserFieldRows, restOfFieldRows) = customFieldRows.span(r => (r.survey_id == curUserRow.survey_id && r.user_id == curUserRow.user_id))

      val curUserRoles = curUserRoleRows.map(_.role)
      val curUserPerms = curUserPermRows.map(_.permission)
      val curUserFields = curUserFieldRows.map(f => (f.name, f.value)).toMap

      val record = SecureUserRecord(curUserRow.user_id, curUserRow.password_hash, curUserRow.password_salt,
        curUserRow.password_hasher, curUserRow.name, curUserRow.email, curUserRow.phone, curUserRoles.toSet, curUserPerms.toSet, curUserFields)

      buildUserRecords(restOfUserRows, restOfRoleRows, restOfPermRows, restOfFieldRows, record +: result)
    }
  }

  lazy val getUsersByRoleQuery = sqlFromResource("admin/get_users_by_role.sql")

  lazy val getRolesByRoleQuery = sqlFromResource("admin/get_roles_by_role.sql")

  lazy val getPermissionsByRoleQuery = sqlFromResource("admin/get_permissions_by_role.sql")

  lazy val getCustomFieldsByRole = sqlFromResource("admin/get_custom_fields_by_role.sql")

  def getUsersByRole(surveyId: Option[String], role: String): Either[LookupError, Seq[SecureUserRecord]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (surveyExists) {

          val userRows = SQL(getUsersByRoleQuery).on('survey_id -> surveyId.getOrElse(""), 'role -> role).executeQuery().as(Macro.namedParser[UserRecordRow].*)

          val roleRows = SQL(getRolesByRoleQuery).on('survey_id -> surveyId.getOrElse(""), 'role -> role).executeQuery().as(Macro.namedParser[RoleRecordRow].*)

          val permRows = SQL(getPermissionsByRoleQuery).on('survey_id -> surveyId.getOrElse(""), 'role -> role).executeQuery().as(Macro.namedParser[PermissionRecordRow].*)

          val customFieldRows = SQL(getCustomFieldsByRole).on('survey_id -> surveyId.getOrElse(""), 'role -> role).executeQuery().as(Macro.namedParser[CustomFieldRecordRow].*)

          Right(buildUserRecords(userRows, roleRows, permRows, customFieldRows))
        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  def getAllUsersInSurvey(surveyId: String): Either[LookupError, Seq[SecureUserRecord]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (surveyExists) {
          val userRows = SQL("SELECT id as user_id, survey_id, password_hash, password_salt, password_hasher FROM users WHERE survey_id = {survey_id} ORDER BY (survey_id, id)").on('surveyId -> surveyId).executeQuery().as(Macro.namedParser[UserRecordRow].*)

          val roleRows = SQL("SELECT survey_id, user_id, role FROM user_roles WHERE survey_id = {survey_id} ORDER BY (survey_id, user_id)").on('survey_id -> surveyId).executeQuery().as(Macro.namedParser[RoleRecordRow].*)

          val permRows = SQL("SELECT survey_id, user_id, permission FROM user_permissions WHERE survey_id = {survey_id} ORDER BY (survey_id, user_id)").on('survey_id -> surveyId).executeQuery().as(Macro.namedParser[PermissionRecordRow].*)

          val customFieldRows = SQL("SELECT survey_id, user_id, name, value FROM user_custom_fields WHERE survey_id = {survey_id} ORDER BY (survey_id, user_id)").on('survey_id -> surveyId).executeQuery().as(Macro.namedParser[CustomFieldRecordRow].*)

          Right(buildUserRecords(userRows, roleRows, permRows, customFieldRows))
        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  lazy val listUsersQuery = sqlFromResource("admin/list_users.sql")

  def listUsers(surveyId: Option[String], offset: Int, limit: Int): Either[LookupError, Seq[PublicUserRecordWithPermissions]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = surveyId match {
          case Some(id) => SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty
          case None => true
        }

        if (surveyExists) {
          val records = SQL(listUsersQuery).on('survey_id -> surveyId.getOrElse(""), 'offset -> offset, 'limit -> limit).executeQuery().as(Macro.namedParser[PublicUserRecordWithPermissionsRow].*).map {
            row =>

              val customFields = row.custom_fields.foldLeft(Map[String, String]()) {
                case (result, Array(name, value)) => result + (name -> value)
              }

              PublicUserRecordWithPermissions(row.survey_id, row.user_id, row.name, row.email, row.phone, customFields, row.roles.toSet, row.permissions.toSet)
          }

          Right(records)
        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  lazy val listUsersByRoleQuery = sqlFromResource("admin/list_users_by_role.sql")


  def listUsersByRole(surveyId: Option[String], role: String, offset: Int, limit: Int): Either[LookupError, Seq[PublicUserRecord]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = surveyId match {
          case Some(id) => SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty
          case None => true
        }

        if (surveyExists) {
          val records = SQL(listUsersByRoleQuery).on('survey_id -> surveyId.getOrElse(""), 'role -> role, 'offset -> offset, 'limit -> limit).executeQuery().as(Macro.namedParser[PublicUserRecordRow].*).map(_.toPublicUserRecord)
          Right(records)
        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  def deleteUsers(surveyId: Option[String], userNames: Seq[String]): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM users WHERE survey_id={survey_id} AND id IN({user_names})").on('survey_id -> surveyId.getOrElse(""), 'user_names -> userNames).execute()

      Right(())
  }

  def nextGeneratedUserId(surveyId: String): Either[UnexpectedDatabaseError, Int] = tryWithConnection {
    implicit conn =>
      Right(SQL("INSERT INTO gen_user_counters VALUES('demo', 0) ON CONFLICT(survey_id) DO UPDATE SET count=gen_user_counters.count+1 RETURNING count")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(SqlParser.int("count").single))
  }

  lazy private val getSurveySupportStaffQuery = sqlFromResource("admin/get_survey_support_staff.sql")

  def getSurveySupportUsers(surveyId: String): Either[UnexpectedDatabaseError, Seq[PublicUserRecord]] = tryWithConnection {
    implicit conn =>
      Right(SQL(getSurveySupportStaffQuery)
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(Macro.namedParser[PublicUserRecordRow].*)
        .map(_.toPublicUserRecord))
  }

  lazy private val getGlobalSupportStaffQuery = sqlFromResource("admin/get_global_support_staff.sql")

  def getGlobalSupportUsers(): Either[UnexpectedDatabaseError, Seq[PublicUserRecord]] = tryWithConnection {
    implicit conn =>
      Right(SQL(getGlobalSupportStaffQuery)
        .executeQuery()
        .as(Macro.namedParser[PublicUserRecordRow].*)
        .map(_.toPublicUserRecord))
  }

}