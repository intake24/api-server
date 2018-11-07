package uk.ac.ncl.openlab.intake24.services.shorturl

import scala.concurrent.Future

trait ShortUrlBackend {
  def shorten(url: Seq[String]): Future[Seq[String]]
}
