package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.errors._

case class NewUserProfile(name: Option[String], email: Option[String], phone: Option[String], roles: Set[String], customFields: Map[String, String])

case class UserProfile(id: Long, name: Option[String], email: Option[String], phone: Option[String], emailNotifications: Boolean, smsNotifications: Boolean, roles: Set[String], customFields: Map[String, String])

case class UserProfileUpdate(name: Option[String], email: Option[String], phone: Option[String], emailNotifications: Boolean, smsNotifications: Boolean, roles: Set[String], customFields: Map[String, String])

case class SecurePassword(hashBase64: String, saltBase64: String, hasher: String)

case class NewUserWithPassword(userInfo: NewUserProfile, password: SecurePassword)

/**
  * A user name in a specific survey's namespace, meant to avoid user name clashes across
  * different surveys.
  */
case class SurveyUserAlias(surveyId: String, userName: String)

case class NewUserWithAlias(alias: SurveyUserAlias, userInfo: NewUserProfile, password: SecurePassword)

trait UserAdminService {

  /**
    * This method is meant for uploading survey respondent data from external files and will
    * automatically create a survey alias.
    */
  def createOrUpdateUsersWithAliases(users: Seq[NewUserWithAlias]): Either[DependentUpdateError, Unit]

  def createUsers(users: Seq[NewUserProfile]): Either[CreateError, Seq[Long]]

  def createUserWithPassword(newUser: NewUserWithPassword): Either[CreateError, Long]

  def updateUser(userId: Long, update: UserProfileUpdate): Either[UpdateError, Unit]

  def getUserById(userId: Long): Either[LookupError, UserProfile]

  def getUserByEmail(email: String): Either[LookupError, UserProfile]

  def getUserByAlias(alias: SurveyUserAlias): Either[LookupError, UserProfile]

  def validateUrlToken(token: String): Either[LookupError, Unit]

  def getUserByUrlToken(token: String): Either[LookupError, UserProfile]


  def deleteUsersById(userIds: Seq[Long]): Either[DeleteError, Unit]

  def deleteUsersByAlias(userAliases: Seq[SurveyUserAlias]): Either[UnexpectedDatabaseError, Unit]


  def getUserPasswordById(userId: Long): Either[LookupError, SecurePassword]

  def getUserPasswordByAlias(alias: SurveyUserAlias): Either[LookupError, SecurePassword]

  def getUserPasswordByEmail(email: String): Either[LookupError, SecurePassword]

  def updateUserPassword(userId: Long, update: SecurePassword): Either[UpdateError, Unit]


  def findUsers(query: String, limit: Int): Either[UnexpectedDatabaseError, Seq[UserProfile]]

  def listUsersByRole(role: String, offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[UserProfile]]

  def getSurveyUserAliases(userIds: Seq[Long], surveyId: String): Either[UnexpectedDatabaseError, Map[Long, String]]

  // Custom data support

  def getCustomUserData(userId: Long): Either[LookupError, Map[String, String]]

  def updateCustomUserData(userId: Long, customUserData: Map[String, String]): Either[UpdateError, Unit]


  // Auto generated users support

  def nextGeneratedUserId(surveyId: String): Either[UnexpectedDatabaseError, Int]
}