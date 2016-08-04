package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError
import uk.ac.ncl.openlab.intake24.GuideImage

trait GuideImageService {
  
  def guideImage(id: String): Either[ResourceError, GuideImage]
}