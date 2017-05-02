package uk.ac.ncl.openlab.intake24.api.shared

import uk.ac.ncl.openlab.intake24.services.systemdb.admin._

case class DeleteUsersRequest(userIds: Seq[Long])

case class DeleteSurveyUsersRequest(userNames: Seq[String])

case class CreateUserRequest(userInfo: NewUserProfile, password: String)

case class PatchUserPasswordRequest(password: String)

case class NewRespondent(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String])

case class CreateOrUpdateSurveyUsersRequest(users: Seq[NewRespondent])

case class UserInfoWithSurveyUserName(id: Long, userName: String, name: Option[String], email: Option[String], phone: Option[String], emailNotifications: Boolean, smsNotifications: Boolean, roles: Set[String], customFields: Map[String, String])

case class CreateRespondentsWithPhysicalDataRequest(users: Seq[NewRespondentWithPhysicalData])

case class CreateRespondentsWithPhysicalDataResponse(userKeys: Seq[NewRespondentIds])
