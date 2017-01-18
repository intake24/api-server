package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.sql.Connection

import scala.Left
import scala.Right
import scala.annotation.tailrec

import anorm._
import javax.inject.Inject
import javax.inject.Named
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SecureUserRecord
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.ParentError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.systemsql.SystemSqlService
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.UpdateError

class UserAdminImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends UserAdminService with SystemSqlService with SqlResourceLoader {

  private def updateUserRolesQuery(surveyId: String, roles: Map[String, Set[String]])(implicit connection: Connection): Either[ParentError, Unit] = {
    SQL("DELETE FROM user_roles WHERE survey_id={survey_id} AND user_id IN ({user_ids})")
      .on('survey_id -> surveyId, 'user_ids -> roles.keySet.toSeq)
      .execute()

    val roleParams = roles.toSeq.flatMap {
      case (userName, roles) =>
        roles.map {
          role =>
            Seq[NamedParameter]('survey_id -> surveyId, 'user_id -> userName, 'role -> role)
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

  private def updateUserPermissionsQuery(surveyId: String, permissions: Map[String, Set[String]])(implicit connection: Connection): Either[ParentError, Unit] = {
    SQL("DELETE FROM user_permissions WHERE survey_id={survey_id} AND user_id IN ({user_ids})")
      .on('survey_id -> surveyId, 'user_ids -> permissions.keySet.toSeq)
      .execute()

    val permissionParams = permissions.toSeq.flatMap {
      case (userName, permissions) =>
        permissions.map {
          permission =>
            Seq[NamedParameter]('survey_id -> surveyId, 'user_id -> userName, 'permission -> permission)
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

  private def updateUserCustomDataQuery(surveyId: String, customData: Map[String, Map[String, String]])(implicit connection: Connection): Either[ParentError, Unit] = {
    SQL("DELETE FROM user_custom_fields WHERE survey_id={survey_id} AND user_id IN ({user_ids})")
      .on('survey_id -> surveyId, 'user_ids -> customData.keySet.toSeq)
      .execute()

    val userCustomFieldParams = customData.toSeq.flatMap {
      case (userName, data) =>
        data.map {
          case (name, value) => Seq[NamedParameter]('survey_id -> surveyId, 'user_id -> userName, 'name -> name, 'value -> value)
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

  def createOrUpdateUsers(surveyId: String, userRecords: Seq[SecureUserRecord]): Either[DependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val userParams = userRecords.map {
          record =>
            Seq[NamedParameter]('id -> record.username, 'survey_id -> surveyId, 'password_hash -> record.passwordHashBase64, 'password_salt -> record.passwordSaltBase64,
              'password_hasher -> record.passwordHasher, 'name -> record.name, 'email -> record.email, 'phone -> record.phone)
        }

        if (userParams.nonEmpty) {
          BatchSql("INSERT INTO users VALUES ({id}, {survey_id}, {password_hash}, {password_salt}, {password_hasher}, {name}, {email}, {phone}) " +
            "ON CONFLICT ON CONSTRAINT users_id_pk DO UPDATE " +
            "SET password_hash={password_hash},password_salt={password_salt},password_hasher={password_hasher},name={name},email={email},phone={phone}", userParams.head, userParams.tail: _*).execute()

          for (
            _ <- updateUserRolesQuery(surveyId, userRecords.foldLeft(Map[String, Set[String]]()) {
              case (acc, record) => acc + (record.username -> record.roles)
            }).right;
            _ <- updateUserPermissionsQuery(surveyId, userRecords.foldLeft(Map[String, Set[String]]()) {
              case (acc, record) => acc + (record.username -> record.permissions)
            }).right;
            _ <- updateUserCustomDataQuery(surveyId, userRecords.foldLeft(Map[String, Map[String, String]]()) {
              case (acc, record) => acc + (record.username -> record.customFields)
            }).right
          ) yield ()
        } else
          Right(())
      }
  }

  def createUser(surveyId: String, userRecord: SecureUserRecord): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        tryWithConstraintCheck[DependentCreateError, Unit]("users_id_pk", e => DuplicateCode(new RuntimeException(s"User name ${userRecord.username} already exists for survey $surveyId"))) {
          SQL("INSERT INTO users VALUES ({id}, {survey_id}, {password_hash}, {password_salt}, {password_hasher}, {name}, {email}, {phone})")
            .on(
              'id -> userRecord.username,
              'survey_id -> surveyId,
              'password_hash -> userRecord.passwordHashBase64,
              'password_salt -> userRecord.passwordSaltBase64,
              'password_hasher -> userRecord.passwordHasher,
              'name -> userRecord.name,
              'email -> userRecord.email,
              'phone -> userRecord.phone)
            .execute()
          for (
            _ <- updateUserRolesQuery(surveyId, Map(userRecord.username -> userRecord.roles)).right;
            _ <- updateUserPermissionsQuery(surveyId, Map(userRecord.username -> userRecord.permissions)).right;
            _ <- updateUserCustomDataQuery(surveyId, Map(userRecord.username -> userRecord.customFields)).right
          ) yield ()
        }
      }
  }

  private case class UserDataRow(name: String, value: String)

  def getCustomUserData(surveyId: String, userId: String): Either[LookupError, Map[String, String]] = tryWithConnection {
    implicit conn =>

      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty
        val userExists = SQL("SELECT 1 FROM users WHERE id={user_id} AND survey_id={survey_id}").on('user_id -> userId, 'survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (!surveyExists)
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
        else if (!userExists)
          Left(RecordNotFound(new RuntimeException(s"User $userId does not exist in survey $surveyId")))
        else {

          val result = SQL("SELECT name, value FROM user_custom_fields WHERE (user_id={user_id} AND survey_id={survey_id})")
            .on('survey_id -> surveyId, 'user_id -> userId)
            .executeQuery().as(Macro.namedParser[UserDataRow].*).map(row => (row.name, row.value)).toMap
          Right(result)
        }
      }
  }

  def updateCustomUserData(surveyId: String, userId: String, data: Map[String, String]): Either[ParentError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty
        val userExists = SQL("SELECT 1 FROM users WHERE id={user_id} AND survey_id={survey_id}").on('user_id -> userId, 'survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

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

  def getUserById(surveyId: String, userId: String): Either[LookupError, SecureUserRecord] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT password_hash, password_salt, password_hasher, name, email, phone FROM users WHERE (survey_id={survey_id} AND id={user_id})")
          .on('survey_id -> surveyId, 'user_id -> userId).executeQuery()
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

  def getUsersByRole(surveyId: String, role: String): Either[LookupError, Seq[SecureUserRecord]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).nonEmpty

        if (surveyExists) {

          val userRows = SQL(getUsersByRoleQuery).on('survey_id -> surveyId, 'role -> role).executeQuery().as(Macro.namedParser[UserRecordRow].*)

          val roleRows = SQL(getRolesByRoleQuery).on('survey_id -> surveyId, 'role -> role).executeQuery().as(Macro.namedParser[RoleRecordRow].*)

          val permRows = SQL(getPermissionsByRoleQuery).on('survey_id -> surveyId, 'role -> role).executeQuery().as(Macro.namedParser[PermissionRecordRow].*)

          val customFieldRows = SQL(getCustomFieldsByRole).on('survey_id -> surveyId, 'role -> role).executeQuery().as(Macro.namedParser[CustomFieldRecordRow].*)

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
}
