package uk.ac.ncl.openlab.intake24.api.shared

import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SurveyUser, SurveyUserAlias, UserInfo, UserInfoWithId}

case class DeleteUsersRequest(userIds: Seq[Long])

case class DeleteSurveyUsersRequest(userNames: Seq[String])

case class CreateUserRequest(userInfo: UserInfo, password: String)

case class CreateOrUpdateSurveyUsersRequest(users: Seq[SurveyUser])
