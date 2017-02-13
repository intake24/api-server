package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.{GuideHeader}
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, GuideImageAdminClient}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.shared.NewGuideImageRequest

class GuideImageAdminClientImpl(apiBaseUrl: String) extends GuideImageAdminClient with ApiResponseParser with HttpRequestUtil {

  val logger = LoggerFactory.getLogger(classOf[GuideImageAdminClientImpl])

  def updateGuideSelectionImage(accessToken: String, id: String, selectionImageId: Long): Either[ApiError, Unit] = {

    val request = getAuthPostRequestNoBody(s"$apiBaseUrl/admin/portion-size/guide-image/selection-screen-image", accessToken)
      .param("id", id)
      .param("selectionImageId", selectionImageId.toString)

    logger.debug("Updating guide selection image")
    logger.debug(request.toString)

    parseApiResponseDiscardBody(request.asString)
  }

  def listGuideImages(accessToken: String): Either[ApiError, Seq[GuideHeader]] = {
    val request = getAuthGetRequestNoBody(s"$apiBaseUrl/admin/portion-size/guide-image", accessToken)
    logger.debug("Listing guide images")
    logger.debug(request.toString)
    parseApiResponse[Seq[GuideHeader]](request.asString)
  }

  def createGuideImage(accessToken: String, image: NewGuideImageRequest): Either[ApiError, Unit] = {
    val request = getAuthPostRequest(s"$apiBaseUrl/admin/portion-size/guide-image/new", accessToken, image)

    logger.debug(s"Creating guide image ${image.id}")

    parseApiResponseDiscardBody(request.asString)
  }
}
