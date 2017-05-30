package uk.ac.ncl.openlab.intake24.api.client.portionsize

import uk.ac.ncl.openlab.intake24.api.client.ApiError
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor

trait ImageAdminClient {

  def processForSelectionScreen(pathPrefix: String, sourceImageId: Long): Either[ApiError, ImageDescriptor]
}
