package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, ImageAdminClient}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor

class ImageAdminClientImpl(apiBaseUrl: String) extends ImageAdminClient with ApiResponseParser with HttpRequestUtil {
  val logger = LoggerFactory.getLogger(classOf[ImageAdminClientImpl])

  override def processForSelectionScreen(authToken: String, pathPrefix: String, sourceImageId: Long): Either[ApiError, ImageDescriptor] = {
    val request = getSimpleHttpAuthPostRequest(s"$apiBaseUrl/admin/images/process-for-selection-screen", authToken)
      .param("pathPrefix", pathPrefix)
      .param("sourceImageId", sourceImageId.toString)

    logger.debug("Processing image for selection screen")
    logger.debug(request.toString)

    parseApiResponse[ImageDescriptor](request.asString)
  }
}
