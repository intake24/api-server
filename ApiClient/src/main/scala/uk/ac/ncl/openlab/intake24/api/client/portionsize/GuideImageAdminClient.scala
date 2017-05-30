package uk.ac.ncl.openlab.intake24.api.client.portionsize

import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.api.client.ApiError
import uk.ac.ncl.openlab.intake24.api.shared.NewGuideImageRequest

trait GuideImageAdminClient {

  def listGuideImages(): Either[ApiError, Seq[GuideHeader]]

  def updateGuideSelectionImage(id: String, selectionImageId: Long): Either[ApiError, Unit]

  def createGuideImage(images: NewGuideImageRequest): Either[ApiError, Unit]
}
