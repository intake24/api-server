package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

case class GuideImageV2 (id: String, description: String, baseImagePath: String, weights: Seq[GuideImageWeightRecord])

case class GuideImageWeightRecord (description: String, objectId: Int, weight: Double)


trait GuideImageService {
  
  def getGuideImage(id: String): Either[LookupError, GuideImageV2]
}