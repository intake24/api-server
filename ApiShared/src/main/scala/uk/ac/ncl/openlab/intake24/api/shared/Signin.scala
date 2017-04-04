package uk.ac.ncl.openlab.intake24.api.shared

case class EmailCredentials(email: String, password: String)

case class SurveyAliasCredentials(surveyId: String, userName: String, password: String)

case class SigninResult(refreshToken: String)

case class RefreshResult(accessToken: String)
