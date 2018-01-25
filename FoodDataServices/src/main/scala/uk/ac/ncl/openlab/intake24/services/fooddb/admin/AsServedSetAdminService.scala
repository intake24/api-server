package uk.ac.ncl.openlab.intake24.services.fooddb.admin


import uk.ac.ncl.openlab.intake24.api.data.AsServedHeader
import uk.ac.ncl.openlab.intake24.errors._

case class AsServedImageWithPaths(sourceId: Long, imagePath: String, thumbnailPath: String, weight: Double)

case class AsServedImageRecord(id: Long, mainImageId: Long, thumbnailId: Long, weight: Double)

case class NewAsServedImageRecord(mainImageId: Long, thumbnailId: Long, weight: Double)

case class AsServedSetWithPaths(id: Long, description: String, images: Seq[AsServedImageWithPaths])

case class AsServedSetRecord(id: Long, description: String, selectionImageId: Long, images: Seq[AsServedImageRecord])

case class NewAsServedSetRecord(id: Long, description: String, selectionImageId: Long, images: Seq[NewAsServedImageRecord])

case class PortableAsServedImage(sourcePath: String, sourceThumbnailPath: String, sourceKeywords: Seq[String], mainImagePath: String, thumbnailPath: String, weight: Double)

// Selection source image is one of the source as served images, avoid creating twice when porting
case class PortableAsServedSet(id: Long, description: String, selectionSourcePath: String, selectionImagePath: String, images: Seq[PortableAsServedImage])

trait AsServedSetsAdminService {

  def listAsServedSets(): Either[UnexpectedDatabaseError, Seq[AsServedHeader]]

  def getAsServedSetWithPaths(id: Long): Either[LookupError, AsServedSetWithPaths]

  def getAsServedSetRecord(id: Long): Either[LookupError, AsServedSetRecord]

  def deleteAsServedSetRecord(id: Long): Either[UnexpectedDatabaseError, Unit]

  def getPortableAsServedSet(id: Long): Either[LookupError, PortableAsServedSet]

  def deleteAllAsServedSets(): Either[UnexpectedDatabaseError, Unit]

  def createAsServedSets(sets: Seq[NewAsServedSetRecord]): Either[CreateError, Unit]

  def updateAsServedSet(id: Long, update: NewAsServedSetRecord): Either[UpdateError, Unit]
}