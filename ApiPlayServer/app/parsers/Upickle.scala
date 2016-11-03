package parsers

import play.api.mvc.BodyParsers
import play.api.http.ContentTypes
import play.api.mvc.Results
import scala.concurrent.Future
import upickle.default._

import play.api.mvc.BodyParser
import upickle.Invalid
import scala.concurrent.ExecutionContext.Implicits.global

object Upickle {

  import BodyParsers.parse._

  private case class ErrorBody(error: String, message: String)

  def upickleRead[T](implicit reader: Reader[T]) =
    when(
      request => request.contentType.exists(_.equalsIgnoreCase(ContentTypes.JSON)),
      tolerantText,
      _ => Future.successful(Results.UnsupportedMediaType)).validate {
        stringBody =>
          try {
            Right(read[T](stringBody))
          } catch {
            case Invalid.Data(_, msg) => Left(Results.BadRequest(write(ErrorBody("json_exception", msg))))
            case Invalid.Json(msg, input) => Left(Results.BadRequest(write(ErrorBody("json_exception", msg))))
            case e: Throwable => Left(Results.BadRequest(write(ErrorBody("json_exception", e.getClass.getName))))
          }
      }
}