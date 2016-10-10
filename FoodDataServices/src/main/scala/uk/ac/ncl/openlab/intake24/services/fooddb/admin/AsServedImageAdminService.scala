package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.AsServedImageService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.AsServedSetV1
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.AsServedHeader

case class AsServedImageUpdate(image_id: Long, weight: Double)

trait AsServedImageAdminService {
  
  def listAsServedSets(): Either[DatabaseError, Map[String, AsServedHeader]]
  
  def deleteAllAsServedSets(): Either[DatabaseError, Unit]

  def createAsServedSets(sets: Seq[AsServedSetV1]): Either[CreateError, Unit]
}