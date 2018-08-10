package uk.ac.ncl.openlab.intake24.play.utils

import io.circe._
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JsonBodyParser @Inject()(playBodyParsers: PlayBodyParsers, implicit val executionContext: ExecutionContext) extends JsonUtils {
  def parse[T](implicit dec: Decoder[T]): BodyParser[T] =
    playBodyParsers.when(
      request => request.contentType.exists(_.equalsIgnoreCase(ContentTypes.JSON)),
      playBodyParsers.raw,
      _ => Future.successful(Results.UnsupportedMediaType)
    ).validate {
      rawBuffer =>
        rawBuffer.asBytes() match {
          case Some(byteString) => parseJson[T](byteString.utf8String)
          case None => Left(Results.EntityTooLarge)
        }
    }
}