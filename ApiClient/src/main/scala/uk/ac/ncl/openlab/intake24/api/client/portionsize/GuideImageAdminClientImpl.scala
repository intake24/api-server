package uk.ac.ncl.openlab.intake24.api.client.portionsize

import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.HttpRequestUtil
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, AuthCache}
import uk.ac.ncl.openlab.intake24.api.shared.NewGuideImageRequest

class GuideImageAdminClientImpl(apiBaseUrl: String, authCache: AuthCache) extends GuideImageAdminClient with ApiResponseParser with HttpRequestUtil {

  val logger = LoggerFactory.getLogger(classOf[GuideImageAdminClientImpl])

  def updateGuideSelectionImage(id: String, selectionImageId: Long): Either[ApiError, Unit] = authCache.withAccessToken {
    accessToken =>

      val request = getAuthPostRequestNoBody(s"$apiBaseUrl/admin/portion-size/guide-image/selection-screen-image", accessToken)
        .param("id", id)
        .param("selectionImageId", selectionImageId.toString)

      logger.debug("Updating guide selection image")
      logger.debug(request.toString)

      parseApiResponseDiscardBody(request.asString)
  }

  def listGuideImages(): Either[ApiError, Seq[GuideHeader]] = authCache.withAccessToken {
    accessToken =>

      val request = getAuthGetRequestNoBody(s"$apiBaseUrl/admin/portion-size/guide-image", accessToken)
      logger.debug("Listing guide images")
      logger.debug(request.toString)
      parseApiResponse[Seq[GuideHeader]](request.asString)
  }

  def createGuideImage(image: NewGuideImageRequest): Either[ApiError, Unit] = authCache.withAccessToken {
    accessToken =>

      val request = getAuthPostRequest(s"$apiBaseUrl/admin/portion-size/guide-image/new", accessToken, image)

      logger.debug(s"Creating guide image ${image.id}")

      parseApiResponseDiscardBody(request.asString)
  }
}
