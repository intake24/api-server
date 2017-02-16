package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors._

case class ImageMapHeader(id: String, description: String)

case class ImageMapObjectRecord(description: String, outline: Array[Double], overlayImageId: Long)

case class NewImageMapRecord(id: String, description: String, baseImageId: Long, navigation: Seq[Int], objects: Map[Int, ImageMapObjectRecord])

trait ImageMapsAdminService {

  def listImageMaps(): Either[UnexpectedDatabaseError, Seq[ImageMapHeader]]

  def getImageMapBaseImageSourceId(id: String): Either[LookupError, Long]

  def createImageMaps(maps: Seq[NewImageMapRecord]): Either[CreateError, Unit]

  def updateImageMap(id: String, update: NewImageMapRecord): Either[UpdateError, Unit]

}