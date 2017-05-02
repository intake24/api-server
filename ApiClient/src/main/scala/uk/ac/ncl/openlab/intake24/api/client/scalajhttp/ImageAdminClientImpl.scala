package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, ImageAdminClient}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor

import io.circe.generic.auto._

class ImageAdminClientImpl(apiBaseUrl: String) extends ImageAdminClient with ApiResponseParser with HttpRequestUtil {
  val logger = LoggerFactory.getLogger(classOf[ImageAdminClientImpl])

  override def processForSelectionScreen(accessToken: String, pathPrefix: String, sourceImageId: Long): Either[ApiError, ImageDescriptor] = {
    val request = getAuthPostRequestNoBody(s"$apiBaseUrl/admin/images/process-for-selection-screen", accessToken)
      .param("pathPrefix", pathPrefix)
      .param("sourceImageId", sourceImageId.toString)

    logger.debug("Processing image for selection screen")
    logger.debug(request.toString)

    parseApiResponse[ImageDescriptor](request.asString)
  }
}
