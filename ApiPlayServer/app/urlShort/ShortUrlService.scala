package urlShort

import scala.concurrent.Future

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */
trait ShortUrlService {
  def shorten(url: String): Future[String]
}
