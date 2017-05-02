package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import java.nio.file.Path

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, ImageMapAdminClient, JsonParser}
import uk.ac.ncl.openlab.intake24.api.shared.NewImageMapRequest
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.ImageMapHeader
import io.circe.generic.auto._

class ImageMapAdminClientImpl(apiBaseUrl: String) extends ImageMapAdminClient with ApiResponseParser with HttpRequestUtil with JsonParser {

  val logger = LoggerFactory.getLogger(classOf[ImageMapAdminClientImpl])

  def createImageMap(accessToken: String, baseImage: Path, svgImage: Path, sourceKeywords: Seq[String], params: NewImageMapRequest): Either[ApiError, Unit] = {

    val paramsJson = toJson(params)

    val req = getAuthPostRequestForm(s"$apiBaseUrl/admin/portion-size/image-map/new-from-svg", accessToken, Seq(("imageMapParameters", paramsJson)),
      Seq(("baseImage", baseImage), ("svg", svgImage)))

    logger.debug(s"Creating image map ${params.id}")
    logger.debug(req.toString)

    parseApiResponseDiscardBody(req.asString)
  }

  def getImageMapBaseImageSourceId(accessToken: String, id: String): Either[ApiError, Long] = {
    parseApiResponse[Long](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/portion-size/image-map/$id/base-image-source-id", accessToken).asString)
  }

  override def listImageMaps(accessToken: String): Either[ApiError, Seq[ImageMapHeader]] = {
    parseApiResponse[Seq[ImageMapHeader]](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/portion-size/image-map", accessToken).asString)
  }
}
