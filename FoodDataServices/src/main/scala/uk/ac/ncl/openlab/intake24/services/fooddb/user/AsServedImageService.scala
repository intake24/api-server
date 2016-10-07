package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

case class UserAsServedImage(url: String, thumbnailUrl: String, weight: Double)

trait AsServedImageService {
  
   def getAsServedSet(id: String): Either[LookupError, Seq[UserAsServedImage]]
}