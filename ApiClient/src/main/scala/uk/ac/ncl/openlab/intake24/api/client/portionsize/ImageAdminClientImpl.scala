package uk.ac.ncl.openlab.intake24.api.client.portionsize

import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.HttpRequestUtil
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, AuthCache}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor

class ImageAdminClientImpl(apiBaseUrl: String, authCache: AuthCache) extends ImageAdminClient with ApiResponseParser with HttpRequestUtil {
  val logger = LoggerFactory.getLogger(classOf[ImageAdminClientImpl])

  override def processForSelectionScreen(pathPrefix: String, sourceImageId: Long): Either[ApiError, ImageDescriptor] = authCache.withAccessToken {
    accessToken =>
      val request = getAuthPostRequestNoBody(s"$apiBaseUrl/admin/images/process-for-selection-screen", accessToken)
        .param("pathPrefix", pathPrefix)
        .param("sourceImageId", sourceImageId.toString)

      logger.debug("Processing image for selection screen")
      logger.debug(request.toString)

      parseApiResponse[ImageDescriptor](request.asString)
  }
}
