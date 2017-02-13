package uk.ac.ncl.openlab.intake24.api.client

import java.nio.file.Path

import uk.ac.ncl.openlab.intake24.api.shared.NewImageMapRequest
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.ImageMapHeader

trait ImageMapAdminClient {

  def listImageMaps(accessToken: String): Either[ApiError, Seq[ImageMapHeader]]

  def getImageMapBaseImageSourceId(accessToken: String, id: String): Either[ApiError, Long]

  def createImageMap(accessToken: String, baseImage: Path, svgImage: Path, sourceKeywords: Seq[String], params: NewImageMapRequest): Either[ApiError, Unit]
}
