package uk.ac.ncl.openlab.intake24.api.shared


case class NewImageMapRequest(id: String, description: String, objectDescriptions: Map[String, String])

case class NewGuideImageRequest(id: String, description: String, imageMapId: String, objectWeights: Map[String, Double])
