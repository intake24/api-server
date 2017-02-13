package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.api.shared.NewGuideImageRequest

trait GuideImageAdminClient {

  def listGuideImages(accessToken: String): Either[ApiError, Seq[GuideHeader]]

  def updateGuideSelectionImage(accessToken: String, id: String, selectionImageId: Long): Either[ApiError, Unit]

  def createGuideImage(accessToken: String, images: NewGuideImageRequest): Either[ApiError, Unit]
}
