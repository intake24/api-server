package parsers

import javax.inject.{Inject, Singleton}

import io.circe._
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