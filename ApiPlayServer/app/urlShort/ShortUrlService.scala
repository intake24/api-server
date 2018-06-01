package urlShort

import scala.concurrent.Future

trait ShortUrlService {
  def shorten(url: Seq[String]): Future[Seq[String]]
}
