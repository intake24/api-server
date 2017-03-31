package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.errors._

case class UserInfo(name: Option[String], email: Option[String], phone: Option[String], roles: Seq[String], customFields: Map[String, String])

case class UserInfoWithId(id: Int, name: Option[String], email: Option[String], phone: Option[String], roles: Seq[String], customFields: Map[String, String])

case class SecurePassword(hashBase64: String, saltBase64: String, hasher: String)

/**
  * A user name in a specific survey's namespace, meant to avoid user name clashes across
  * different surveys.
  */
case class SurveyUserAlias(surveyId: String, userName: String)

case class NewUserWithAlias(alias: SurveyUserAlias, userInfo: UserInfo, password: SecurePassword)


trait UserAdminService {

  /**
    * This function meant for uploading survey respondent data from external files and will
    * automatically create a survey alias.
    */
  def createOrUpdateUsersWithAliases(users: Seq[NewUserWithAlias]): Either[DependentUpdateError, Unit]

  def createUser(userInfo: UserInfo): Either[DependentCreateError, UserInfoWithId]

  def updateUser(userId: Int, newRecord: UserInfo): Either[DependentUpdateError, UserInfoWithId]


  def getUserById(userId: Int): Either[LookupError, UserInfoWithId]

  def getUserByAlias(alias: SurveyUserAlias): Either[LookupError, UserInfoWithId]

  def deleteUsers(userIds: Seq[Int]): Either[DeleteError, Unit]


  def getUserPasswordById(userId: Int): Either[LookupError, SecurePassword]

  def updateUserPassword(userId: Int, update: SecurePassword): Either[UpdateError, Unit]

  def getUserPasswordByAlias(surveyId: String, userName: String): Either[LookupError, SecurePassword]


  def listGlobalUsers(offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]]

  def listUsersBySurvey(surveyId: String, offset: Int, limit: Int): Either[LookupError, Seq[UserInfoWithId]]

  def listUsersByRole(role: String, offset: Int, limit: Int): Either[LookupError, Seq[UserInfo]]

  // Custom data support

  def getCustomUserData(userId: Int): Either[LookupError, Map[String, String]]

  def updateCustomUserData(userId: Int, customUserData: Map[String, String]): Either[ParentError, Unit]


  // Auto generated users support

  def nextGeneratedUserId(surveyId: String): Either[UnexpectedDatabaseError, Int]

  // Support users

  def getSurveySupportUsers(surveyId: String): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]]

  def getGlobalSupportUsers(): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]]

  /*  def updateGlobalSupportUsers(supportUsers: Seq[UserRef]): Either[ParentError, Unit]

    def updateSupportUsersForSurvey(surveyId: String, supportUsers: Seq[UserRef]): Either[ParentError, Unit] */
}