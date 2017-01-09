package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import java.nio.file.Path

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, ImageMapAdminClient}
import uk.ac.ncl.openlab.intake24.api.shared.NewImageMapRequest
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.ImageMapHeader
import upickle.default._

class ImageMapAdminClientImpl(apiBaseUrl: String) extends ImageMapAdminClient with ApiResponseParser with HttpRequestUtil {

  val logger = LoggerFactory.getLogger(classOf[ImageMapAdminClientImpl])

  def createImageMap(authToken: String, baseImage: Path, svgImage: Path, sourceKeywords: Seq[String], params: NewImageMapRequest): Either[ApiError, Unit] = {

    val paramsJson = write(params).toString

    val req = getAuthFormPostRequest(s"$apiBaseUrl/admin/portion-size/image-map/new-from-svg", authToken, Seq(("imageMapParameters", paramsJson)),
      Seq(("baseImage", baseImage), ("svg", svgImage)))

    logger.debug(s"Creating image map ${params.id}")
    logger.debug(req.toString)

    parseApiResponseDiscardBody(req.asString)
  }

  def getImageMapBaseImageSourceId(authToken: String, id: String): Either[ApiError, Long] = {
    parseApiResponse[Long](getSimpleHttpAuthGetRequest(s"$apiBaseUrl/admin/portion-size/image-map/$id/base-image-source-id", authToken).asString)
  }

  override def listImageMaps(authToken: String): Either[ApiError, Seq[ImageMapHeader]] = {
    parseApiResponse[Seq[ImageMapHeader]](getSimpleHttpAuthGetRequest(s"$apiBaseUrl/admin/portion-size/image-map", authToken).asString)
  }
}
