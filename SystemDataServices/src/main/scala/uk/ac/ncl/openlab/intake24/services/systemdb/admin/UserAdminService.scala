package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.services.systemdb.errors.{DependentCreateError, DependentUpdateError, LookupError, ParentError}

case class SecureUserRecord(username: String, passwordHashBase64: String, passwordSaltBase64: String, passwordHasher: String,
                            name: Option[String], email: Option[String], phone: Option[String],
                            roles: Set[String], permissions: Set[String], customFields: Map[String, String])

case class UserRef(surveyId: String, userId: String)

trait UserAdminService {

  def getCustomUserData(surveyId: Option[String], userId: String): Either[LookupError, Map[String, String]]

  def updateCustomUserData(surveyId: Option[String], userId: String, userData: Map[String, String]): Either[ParentError, Unit]

  def createOrUpdateUsers(surveyId: Option[String], userRecords: Seq[SecureUserRecord]): Either[DependentUpdateError, Unit]

  def createUser(surveyId: Option[String], userRecord: SecureUserRecord): Either[DependentCreateError, Unit]

  def getAllUsersInSurvey(surveyId: String): Either[LookupError, Seq[SecureUserRecord]]

  def getUserById(surveyId: Option[String], name: String): Either[LookupError, SecureUserRecord]

  def getUsersByRole(surveyId: Option[String], role: String): Either[LookupError, Seq[SecureUserRecord]]

/*  def updateGlobalSupportUsers(supportUsers: Seq[UserRef]): Either[ParentError, Unit]

  def updateSupportUsersForSurvey(surveyId: String, supportUsers: Seq[UserRef]): Either[ParentError, Unit] */
}
