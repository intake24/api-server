package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import scala.collection.JavaConverters._

case class UserGuideImage(description: String, imageMapId: String, weights: Map[Int, Double]) {
  def weightsAsJavaMap =
    weights.map {
      case (k, v) => new java.lang.Integer(k) -> new java.lang.Double(v)
    }.asJava
}

trait GuideImageService {

  def getGuideImage(id: String): Either[LookupError, UserGuideImage]
}