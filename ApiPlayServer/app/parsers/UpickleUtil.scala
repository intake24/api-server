package parsers

import play.api.mvc.{BodyParser, BodyParsers, Result, Results}
import play.api.http.ContentTypes

import scala.concurrent.Future
import upickle.default._
import upickle.Invalid

import scala.concurrent.ExecutionContext.Implicits.global

trait UpickleUtil {

  import BodyParsers.parse._

  private case class ErrorBody(error: String, message: String)

  def parseJson[T](json: String)(implicit reader: Reader[T]): Either[Result, T] = {
    try {
      Right(read[T](json))
    } catch {
      case Invalid.Data(_, msg) => Left(Results.BadRequest(write(ErrorBody("json_exception", msg))))
      case Invalid.Json(msg, _) => Left(Results.BadRequest(write(ErrorBody("json_exception", msg))))
      case e: Throwable => Left(Results.BadRequest(write(ErrorBody("json_exception", s"${e.getClass.getName}: ${e.getMessage}"))))
    }
  }

  def withParsedValue[T](json: String)(block: T => Result)(implicit reader: Reader[T]): Result =
    parseJson[T](json) match {
      case Right(value) => block(value)
      case Left(errorResult) => errorResult
    }

  def upickleBodyParser[T](implicit reader: Reader[T]): BodyParser[T] =
    when(
      request => request.contentType.exists(_.equalsIgnoreCase(ContentTypes.JSON)),
      tolerantText,
      _ => Future.successful(Results.UnsupportedMediaType)).validate {
      stringBody =>
        parseJson[T](stringBody)
    }
}