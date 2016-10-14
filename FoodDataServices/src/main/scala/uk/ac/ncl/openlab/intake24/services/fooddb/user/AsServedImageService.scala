package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.AsServedImageV1

case class UserAsServedImage(mainImagePath: String, thumbnailPath: String, weight: Double)

trait AsServedImageService {
  
   def getAsServedSet(id: String): Either[LookupError, Seq[UserAsServedImage]]
}