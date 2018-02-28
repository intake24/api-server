package urlShort

import com.google.inject.Inject
import io.circe._
import io.circe.parser._
import play.api.{Configuration, Logger}
import play.api.libs.ws.WSClient
import uk.ac.ncl.openlab.intake24.services.systemdb.shortUrls.ShortUrlDataService

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */
class ShortUrlServiceImpl @Inject()(ws: WSClient,
                                    dataService: ShortUrlDataService,
                                    executionContext: ExecutionContext,
                                    configuration: Configuration) extends ShortUrlService {

  private implicit val implicitEC = executionContext

  private implicit val responseDecoder =
    Decoder.forProduct3[String, String, String, ShortResp]("kind", "id", "longUrl") {
      (kind, id, longUrl) => ShortResp(kind, id, longUrl)
    }

  private case class ShortResp(kind: String, id: String, longUrl: String)

  override def shorten(url: String): Future[String] = {
    dataService.getShortUrl(url) match {
      case Right(shortUrl) => Future(shortUrl)
      case Left(e) => requestShortUrl(url).map { shUrl =>
        dataService.createShortUrl(url, shUrl)
        shUrl
      }
    }
  }

  private def requestShortUrl(longUrl: String): Future[String] = {
    ws.url(s"https://www.googleapis.com/urlshortener/v1/url?key=${configuration.get[String]("intake24.urlShort.apiKey")}")
      .withHttpHeaders("Content-Type" -> "application/json; charset=utf-8")
      .withBody(s"""{"longUrl": "$longUrl"}""")
      .execute("POST")
      .map { response =>
        if (response.status == 200)
          decode[ShortResp](response.body) match {
            case Right(res) => res.id
            case Left(e) => throw new Exception(s"Couldn't parse response from Google short url: ${response.body}")
          }
        else {
          Logger.error(s"${getClass.getSimpleName}. Google short url request failed with status ${response.status}: ${response.body}")
          throw new Exception(s"Google short url request failed with status ${response.status}: ${response.body}")
        }
      }
  }

}
