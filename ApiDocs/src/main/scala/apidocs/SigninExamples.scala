package apidocs

import uk.ac.ncl.openlab.intake24.api.shared.{SurveyAliasCredentials, RefreshResult, SigninResult}

object SigninExamples {

  import JSONPrettyPrinter._

  val signInRequest = asPrettyJSON(SurveyAliasCredentials(Some("my_survey"), "staff1", "password123"))

  val signInResult = asPrettyJSON(SigninResult("(refresh token in JWT format)"))

  //val refreshRequest

  val refreshResult = asPrettyJSON(RefreshResult("(access token in JWT format)"))


}
