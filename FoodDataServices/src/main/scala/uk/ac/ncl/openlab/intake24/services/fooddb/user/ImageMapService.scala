package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import scala.collection.JavaConverters._

case class UserImageMapObject(id: Int, description: String, overlayPath: String, outline: Array[Double])

case class UserImageMap(baseImagePath: String, objects: Seq[UserImageMapObject])

trait ImageMapService {

  def getImageMap(id: String): Either[LookupError, UserImageMap]
  def getImageMaps(id: Seq[String]): Either[LookupError, Seq[UserImageMap]]
}