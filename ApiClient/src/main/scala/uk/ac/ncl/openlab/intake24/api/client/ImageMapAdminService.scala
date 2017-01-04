package uk.ac.ncl.openlab.intake24.api.client

import java.nio.file.Path

import uk.ac.ncl.openlab.intake24.api.shared.NewImageMapRequest

trait ImageMapAdminService {

  def createImageMap(authToken: String, baseImage: Path, svgImage: Path, sourceKeywords: Seq[String], params: NewImageMapRequest): Either[ApiError, Unit]
}
