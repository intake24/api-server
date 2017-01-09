package uk.ac.ncl.openlab.intake24.api.shared

case class Credentials(survey_id: String, username: String, password: String)

case class AuthToken(token: String)

case class ErrorDescription(errorCode: String, errorMessage: String)

case class NewImageMapRequest(id: String, description: String, objectDescriptions: Map[String, String])

case class NewGuideImageRequest(id: String, description: String, imageMapId: String, objectWeights: Map[String, Double])
