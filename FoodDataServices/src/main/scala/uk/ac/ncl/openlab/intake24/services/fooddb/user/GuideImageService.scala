package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.errors.LookupError

import scala.collection.JavaConverters._

case class UserGuideImage(description: String, imageMapId: String, weights: Map[Int, Double]) {
  def weightsAsJavaMap: java.util.Map[java.lang.Integer, java.lang.Double] =
    new java.util.HashMap[java.lang.Integer, java.lang.Double](
      weights.map {
        case (k, v) => new java.lang.Integer(k) -> new java.lang.Double(v)
      }.asJava)
}

trait GuideImageService {

  def getGuideImage(id: String): Either[LookupError, UserGuideImage]
}
