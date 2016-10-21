package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.AsServedImageService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError

case class AsServedImageWithPaths(mainImageId: Long, mainImagePath: String, thumbnailId: Long, thumbnailPath: String, weight: Double)

case class AsServedImage(mainImageId: Long, thumbnailId: Long, weight: Double)

case class AsServedSetWithPaths(id: String, description: String, images: Seq[AsServedImageWithPaths])

case class AsServedSet(id: String, description: String, images: Seq[AsServedImage])

case class PortableAsServedImage(sourcePath: String, sourceKeywords: Seq[String], mainImagePath: String, thumbnailPath: String, weight: Double)

case class PortableAsServedSet(id: String, description: String, images: Seq[PortableAsServedImage])

trait AsServedImageAdminService {
  
  def listAsServedSets(): Either[DatabaseError, Map[String, AsServedHeader]]
  
  def getAsServedSet(id: String): Either[LookupError, AsServedSetWithPaths]  
  
  def getPortableAsServedSet(id: String): Either[LookupError, PortableAsServedSet]
  
  def deleteAllAsServedSets(): Either[DatabaseError, Unit]

  def createAsServedSets(sets: Seq[AsServedSet]): Either[CreateError, Unit]
  
  def updateAsServedSet(id: String, update: AsServedSet): Either[UpdateError, Unit]
}