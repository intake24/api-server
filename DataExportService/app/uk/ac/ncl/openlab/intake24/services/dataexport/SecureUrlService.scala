package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io.File
import java.net.URL
import java.time.ZonedDateTime

import scala.util.Try

trait SecureUrlService {

  def createUrl(fileName: String, file: File, expirationDate: ZonedDateTime): Try[URL]
}
