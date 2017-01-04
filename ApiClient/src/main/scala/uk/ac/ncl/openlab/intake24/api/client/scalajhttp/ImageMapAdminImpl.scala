package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import java.nio.file.Path

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, ImageMapAdminService}
import uk.ac.ncl.openlab.intake24.api.shared.NewImageMapRequest

import upickle.default._

class ImageMapAdminImpl(apiBaseUrl: String) extends ImageMapAdminService with ApiResponseParser with HttpRequestUtil {

  def createImageMap(authToken: String, baseImage: Path, svgImage: Path, sourceKeywords: Seq[String], params: NewImageMapRequest): Either[ApiError, Unit] = {

    val paramsJson = write(params).toString

    val req = getAuthFormPostRequest(s"$apiBaseUrl/admin/portion-size/image-map/new-from-svg", authToken, Seq(("imageMapParameters", paramsJson)),
      Seq(("baseImage", baseImage), ("svg", svgImage)))

    parseApiResponseDiscardBody(req.asString)
  }
}
