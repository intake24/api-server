package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import scala.collection.JavaConverters._

case class ImageMapObject(id: Int, overlayPath: String, outline: Array[Double])

case class ImageMap(baseImagePath: String, navigation: Array[Int], objects: Seq[ImageMapObject])

case class UserGuideImage(description: String, imageMap: ImageMap, objects: Map[Int, GuideImageObject]) {
  def objectsAsJavaMap =
    objects.map {
      case (k, v) => new java.lang.Integer(k) -> v
    }.asJava
}

case class GuideImageObject(description: String, weight: Double)

trait GuideImageService {

  def getGuideImage(id: String): Either[LookupError, UserGuideImage]
}