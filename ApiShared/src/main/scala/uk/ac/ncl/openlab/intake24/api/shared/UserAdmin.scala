package uk.ac.ncl.openlab.intake24.api.shared

import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SurveyUser, SurveyUserAlias, UserInfoWithId}

case class DeleteUsersRequest(userIds: Seq[Long])

case class DeleteSurveyUsersRequest(surveyId: String, userNames: Seq[String])

case class CreateUserRequest(newUsers: Seq[UserInfoWithId])

case class CreateOrUpdateSurveyUsersRequest(users: Seq[SurveyUser])
