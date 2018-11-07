package uk.ac.ncl.openlab.intake24.shorturls

import cats.effect.{ContextShift, IO, Timer}
import io.circe.generic.auto._
import javax.inject.{Inject, Singleton}
import org.http4s.Method._
import org.http4s.headers._
import org.http4s.{Header, Headers, MediaType, Uri}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.io._

import scala.concurrent.ExecutionContext.Implicits.global

case class ShortUrlsHttpClientConfig(serviceBaseUrl: Uri)

@Singleton
class ShortUrlsHttpClient @Inject()(val config: ShortUrlsHttpClientConfig) extends ShortUrlsClient {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val (client, release) = BlazeClientBuilder[IO](global).allocate.unsafeRunSync

  override def getShortUrls(request: ShortUrlsRequest): IO[ShortUrlsResponse] = {
    client.expect[ShortUrlsResponse](POST(config.serviceBaseUrl.withPath("/shorten"), request))
  }
}
