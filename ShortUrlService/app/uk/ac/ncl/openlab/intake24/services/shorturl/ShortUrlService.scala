package uk.ac.ncl.openlab.intake24.services.shorturl

import scala.concurrent.Future

trait ShortUrlService {
  def shorten(url: Seq[String]): Future[Seq[String]]
}
