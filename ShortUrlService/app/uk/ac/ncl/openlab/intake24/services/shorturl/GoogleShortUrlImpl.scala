package uk.ac.ncl.openlab.intake24.services.shorturl

import com.google.inject.Inject
import io.circe._
import io.circe.parser._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}

class GoogleShortUrlImpl @Inject()(ws: WSClient,
                                   implicit val executionContext: ExecutionContext,
                                   configuration: Configuration) extends ShortUrlBackend {

  private implicit val responseDecoder =
    Decoder.forProduct3[String, String, String, ShortResp]("kind", "id", "longUrl") {
      (kind, id, longUrl) => ShortResp(kind, id, longUrl)
    }

  private val apiKey = configuration.get[String]("intake24.urlShort.google.apiKey")

  private case class ShortResp(kind: String, id: String, longUrl: String)

  override def shorten(urls: Seq[String]): Future[Seq[String]] = Future.traverse(urls)(requestShortUrl)

  private def requestShortUrl(longUrl: String): Future[String] = {
    ws.url(s"https://www.googleapis.com/urlshortener/v1/url?key=${apiKey}")
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
