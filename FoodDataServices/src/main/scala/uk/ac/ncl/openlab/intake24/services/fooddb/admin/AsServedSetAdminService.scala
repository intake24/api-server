package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.errors._

case class AsServedImageWithPaths(sourceId: Long, imagePath: String, thumbnailPath: String, weight: Double)

case class AsServedImageRecord(id: Long, mainImageId: Long, thumbnailId: Long, weight: Double)

case class NewAsServedImageRecord(mainImageId: Long, thumbnailId: Long, weight: Double)

case class AsServedSetWithPaths(id: String, description: String, images: Seq[AsServedImageWithPaths])

case class AsServedSetRecord(id: String, description: String, selectionImageId: Long, images: Seq[AsServedImageRecord])

case class NewAsServedSetRecord(id: String, description: String, selectionImageId: Long, images: Seq[NewAsServedImageRecord])

case class PortableAsServedImage(sourcePath: String, sourceThumbnailPath: String, sourceKeywords: Seq[String], mainImagePath: String, thumbnailPath: String, weight: Double)

// Selection source image is one of the source as served images, avoid creating twice when porting
case class PortableAsServedSet(id: String, description: String, selectionSourcePath: String, selectionImagePath: String, images: Seq[PortableAsServedImage])

trait AsServedSetsAdminService {

  def listAsServedSets(): Either[UnexpectedDatabaseError, Map[String, AsServedHeader]]

  def getAsServedSetWithPaths(id: String): Either[LookupError, AsServedSetWithPaths]

  def getAsServedSetRecord(id: String): Either[LookupError, AsServedSetRecord]

  def deleteAsServedSetRecord(id: String): Either[UnexpectedDatabaseError, Unit]

  def getPortableAsServedSet(id: String): Either[LookupError, PortableAsServedSet]

  def deleteAllAsServedSets(): Either[UnexpectedDatabaseError, Unit]

  def createAsServedSets(sets: Seq[NewAsServedSetRecord]): Either[CreateError, Unit]

  def updateAsServedSet(id: String, update: NewAsServedSetRecord): Either[UpdateError, Unit]
}