package apidocs

import uk.ac.ncl.openlab.intake24.api.shared.{EmailCredentials, RefreshResult, SigninResult, SurveyAliasCredentials}

object SigninExamples {

  import JSONPrettyPrinter._

  val aliasSigninRequest = asPrettyJSON(SurveyAliasCredentials("my_survey", "user1", "password123"))

  val emailSigninRequest = asPrettyJSON(EmailCredentials("john@smith.com", "password123"))

  val signInResult = asPrettyJSON(SigninResult("(refresh token in JWT format)"))

  //val refreshRequest

  val refreshResult = asPrettyJSON(RefreshResult("(access token in JWT format)"))


}
