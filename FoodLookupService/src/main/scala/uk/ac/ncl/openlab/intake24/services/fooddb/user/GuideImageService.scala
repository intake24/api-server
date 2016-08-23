package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.GuideImage

trait GuideImageService {
  
  def getGuideImage(id: String): Either[LookupError, GuideImage]
}