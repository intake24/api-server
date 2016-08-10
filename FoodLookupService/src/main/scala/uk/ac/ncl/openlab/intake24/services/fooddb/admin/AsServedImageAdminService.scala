package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.AsServedImageService
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.AsServedSet

trait AsServedImageAdminService extends AsServedImageService {
  
  def allAsServedSets(): Either[DatabaseError, Seq[AsServedHeader]]
  
  def deleteAllAsServedSets(): Either[DatabaseError, Unit]

  def createAsServedSets(sets: Seq[AsServedSet]): Either[DatabaseError, Unit]
}