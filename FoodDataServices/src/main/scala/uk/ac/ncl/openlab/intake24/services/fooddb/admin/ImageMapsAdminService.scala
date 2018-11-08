package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors._

case class ImageMapHeader(id: String, description: String)

case class NewImageMapObject(id: Int, description: String, navigationIndex: Int, outline: Array[Double], overlayImageId: Long)

case class NewImageMapRecord(id: String, description: String, baseImageId: Long, navigation: Seq[Int], objects: Map[Int, NewImageMapObject])

case class ImageMapRecord(id: String, description: String, baseImageId: Long, baseImagePath: String, objects: Seq[ImageMapObject])

case class ImageMapObject(id: Int, description: String, overlayImageId: Long, overlayImagePath: String, navigationIndex: Int, outlineCoordinates: Array[Double])

case class ImageMapMeta(id: String, description: String)

trait ImageMapsAdminService {

  def listImageMaps(): Either[UnexpectedDatabaseError, Seq[ImageMapHeader]]

  def getImageMapBaseImageSourceId(id: String): Either[LookupError, Long]

  def createImageMaps(maps: Seq[NewImageMapRecord]): Either[CreateError, Unit]

  def updateImageMapMeta(id: String, meta: ImageMapMeta): Either[UpdateError, Unit]

  def updateImageMapObjects(id: String, objects: Seq[NewImageMapObject]): Either[UpdateError, Unit]

  def updateImageMap(id: String, update: NewImageMapRecord): Either[UpdateError, Unit]

  def getImageMap(id: String): Either[LookupError, ImageMapRecord]

}