package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.services.systemdb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.ParentError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.services.systemdb.errors.DependentCreateError

case class SecureUserRecord(username: String, passwordHashBase64: String, passwordSaltBase64: String, passwordHasher: String, roles: Set[String], permissions: Set[String], customFields: Map[String, String])

trait UserAdminService {

  def getCustomUserData(surveyId: String, userId: String): Either[LookupError, Map[String, String]]

  def updateCustomUserData(surveyId: String, userId: String, userData: Map[String, String]): Either[ParentError, Unit]

  def createOrUpdateUsers(surveyId: String, userRecords: Seq[SecureUserRecord]): Either[DependentUpdateError, Unit]

  def createUser(surveyId: String, userRecord: SecureUserRecord): Either[DependentCreateError, Unit]

  def getAllUsersInSurvey(surveyId: String): Either[LookupError, Seq[SecureUserRecord]]

  def getUserById(surveyId: String, name: String): Either[LookupError, SecureUserRecord]

  def getUsersByRole(surveyId: String, role: String): Either[LookupError, Seq[SecureUserRecord]]
}
