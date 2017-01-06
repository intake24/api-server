package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor

trait ImageAdminClient {

  def processForSelectionScreen(authToken: String, pathPrefix: String, sourceImageId: Long): Either[ApiError, ImageDescriptor]
}
