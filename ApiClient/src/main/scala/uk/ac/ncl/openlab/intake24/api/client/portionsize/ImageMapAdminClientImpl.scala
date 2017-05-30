package uk.ac.ncl.openlab.intake24.api.client.portionsize

import java.nio.file.Path

import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.client._
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.HttpRequestUtil
import uk.ac.ncl.openlab.intake24.api.shared.NewImageMapRequest
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.ImageMapHeader

class ImageMapAdminClientImpl(apiBaseUrl: String, authCache: AuthCache) extends ImageMapAdminClient with ApiResponseParser with HttpRequestUtil with JsonParser {

  val logger = LoggerFactory.getLogger(classOf[ImageMapAdminClientImpl])

  def createImageMap(baseImage: Path, svgImage: Path, sourceKeywords: Seq[String], params: NewImageMapRequest): Either[ApiError, Unit] = authCache.withAccessToken {
    accessToken =>
      val paramsJson = toJson(params)

      val req = getAuthPostRequestForm(s"$apiBaseUrl/admin/portion-size/image-map/new-from-svg", accessToken, Seq(("imageMapParameters", paramsJson)),
        Seq(("baseImage", baseImage), ("svg", svgImage)))

      logger.debug(s"Creating image map ${params.id}")
      logger.debug(req.toString)

      parseApiResponseDiscardBody(req.asString)
  }

  def getImageMapBaseImageSourceId(id: String): Either[ApiError, Long] = authCache.withAccessToken {
    accessToken =>
      parseApiResponse[Long](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/portion-size/image-map/$id/base-image-source-id", accessToken).asString)
  }

  override def listImageMaps(): Either[ApiError, Seq[ImageMapHeader]] = authCache.withAccessToken {
    accessToken =>
      parseApiResponse[Seq[ImageMapHeader]](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/portion-size/image-map", accessToken).asString)
  }
}
