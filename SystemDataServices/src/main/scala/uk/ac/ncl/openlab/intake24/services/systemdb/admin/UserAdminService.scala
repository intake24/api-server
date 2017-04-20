package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.errors._

case class UserInfo(name: Option[String], email: Option[String], phone: Option[String], emailNotifications: Boolean, smsNotifications: Boolean, roles: Set[String], customFields: Map[String, String])

case class UserInfoWithId(id: Long, name: Option[String], email: Option[String], phone: Option[String], emailNotifications: Boolean, smsNotifications: Boolean, roles: Set[String], customFields: Map[String, String])

case class SecurePassword(hashBase64: String, saltBase64: String, hasher: String)

case class NewUserWithPassword(userInfo: UserInfo, password: SecurePassword)


/**
  * A user name in a specific survey's namespace, meant to avoid user name clashes across
  * different surveys.
  */
case class SurveyUserAlias(surveyId: String, userName: String)

case class NewUserWithAlias(alias: SurveyUserAlias, userInfo: UserInfo, password: SecurePassword)

case class SurveyUser(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String])

case class SurveyRespondentWithUrlToken(token: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String])

case class SecurePasswordForId(userId: Long, password: SecurePassword)

trait UserAdminService {

  /**
    * This function meant for uploading survey respondent data from external files and will
    * automatically create a survey alias.
    */
  def createOrUpdateUsersWithAliases(users: Seq[NewUserWithAlias]): Either[DependentUpdateError, Unit]

  def createUsers(users: Seq[UserInfo]): Either[CreateError, Seq[Long]]

  def createUserWithPassword(newUser: NewUserWithPassword): Either[CreateError, Long]

  def updateUser(userId: Long, newRecord: UserInfo): Either[UpdateError, Unit]


  def getUserById(userId: Long): Either[LookupError, UserInfoWithId]

  def getUserByEmail(email: String): Either[LookupError, UserInfoWithId]

  def getUserByAlias(alias: SurveyUserAlias): Either[LookupError, UserInfoWithId]

  def validateUrlToken(token: String): Either[LookupError, Unit]

  def getUserByUrlToken(token: String): Either[LookupError, UserInfoWithId]

  def deleteUsersById(userIds: Seq[Long]): Either[UnexpectedDatabaseError, Unit]

  def deleteUsersByAlias(userAliases: Seq[SurveyUserAlias]): Either[UnexpectedDatabaseError, Unit]


  def getUserPasswordById(userId: Long): Either[LookupError, SecurePassword]

  def getUserPasswordByAlias(alias: SurveyUserAlias): Either[LookupError, SecurePassword]

  def getUserPasswordByEmail(email: String): Either[LookupError, SecurePassword]

  def updateUserPassword(userId: Long, update: SecurePassword): Either[UpdateError, Unit]


  def findUsers(query: String, limit: Int): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]]

  def listUsersByRole(role: String, offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[UserInfoWithId]]

  def getSurveyUserAliases(userIds: Seq[Long], surveyId: String): Either[UnexpectedDatabaseError, Map[Long, String]]

  // Custom data support

  def getCustomUserData(userId: Long): Either[LookupError, Map[String, String]]

  def updateCustomUserData(userId: Long, customUserData: Map[String, String]): Either[UpdateError, Unit]


  // Auto generated users support

  def nextGeneratedUserId(surveyId: String): Either[UnexpectedDatabaseError, Int]
}