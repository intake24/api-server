package uk.ac.ncl.openlab.intake24.services.shorturl

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.Configuration

import scala.concurrent.Future
import scala.util.Random

@Singleton
class InternalShortUrlImpl @Inject()(configuration: Configuration) extends ShortUrlService {

  private val random = new Random()
  private val alphabet = configuration.get[String]("intake24.urlShort.internal.alphabet")
  private val domain = configuration.get[String]("intake24.urlShort.internal.domain")
  private val length = configuration.get[Int]("intake24.urlShort.internal.length")

  private def generateToken: String = {
    val sb = new StringBuffer()

    1.to(length).foreach {
      _ =>
        sb.append(alphabet.charAt(random.nextInt(alphabet.length)))
    }

    sb.toString
  }

  def shorten(url: Seq[String]): Future[Seq[String]] = Future.successful {
    url.map {
      _ => domain + "/" + generateToken
    }
  }
}
