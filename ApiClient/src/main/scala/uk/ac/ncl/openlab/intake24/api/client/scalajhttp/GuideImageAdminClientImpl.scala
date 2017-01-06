package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, GuideImageAdminClient}

import org.slf4j.LoggerFactory

class GuideImageAdminClientImpl(apiBaseUrl: String) extends GuideImageAdminClient with ApiResponseParser with HttpRequestUtil {

  val logger = LoggerFactory.getLogger(classOf[GuideImageAdminClientImpl])

  def updateGuideSelectionImage(authToken: String, id: String, selectionImageId: Long): Either[ApiError, Unit] = {

    val request = getSimpleHttpAuthPostRequest(s"$apiBaseUrl/admin/portion-size/guide-image/selection-screen-image", authToken)
      .param("id", id)
      .param("selectionImageId", selectionImageId.toString)

    logger.debug("Updating guide selection image")
    logger.debug(request.toString)

    parseApiResponseDiscardBody(request.asString)
  }

  def listGuideImages(authToken: String): Either[ApiError, Seq[GuideHeader]] = {
    val request = getSimpleHttpAuthGetRequest(s"$apiBaseUrl/admin/portion-size/guide-image", authToken)
    logger.debug("Listing guide images")
    logger.debug(request.toString)
    parseApiResponse[Seq[GuideHeader]](request.asString)
  }
}
