package uk.ac.ncl.openlab.intake24.api.client.portionsize

import java.nio.file.Path

import uk.ac.ncl.openlab.intake24.api.client.ApiError
import uk.ac.ncl.openlab.intake24.api.shared.NewImageMapWithObjectsRequest
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.ImageMapHeader

trait ImageMapAdminClient {

  def listImageMaps(): Either[ApiError, Seq[ImageMapHeader]]

  def getImageMapBaseImageSourceId(id: String): Either[ApiError, Long]

  def createImageMap(baseImage: Path, svgImage: Path, sourceKeywords: Seq[String], params: NewImageMapWithObjectsRequest): Either[ApiError, Unit]
}
