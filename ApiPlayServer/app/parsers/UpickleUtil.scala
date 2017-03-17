package parsers

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZonedDateTime}

import play.api.http.ContentTypes
import play.api.mvc.{BodyParser, BodyParsers, Result, Results}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import upickle.{Invalid, Js}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UpickleUtil {

  import BodyParsers.parse._
  import upickle.default._

  /*protected implicit val instantWriter = Writer[Instant] {
    case t => Js.Str(DateTimeFormatter.ISO_INSTANT.format(t))
  }

  protected implicit val instantReader = Reader[Instant] {
    case Js.Str(v) => Instant.parse(v)
  }*/

  protected implicit val offsetDateTimeRW = ReadWriter[ZonedDateTime](
    t => Js.Str(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(t)), {
      case Js.Str(v) => OffsetDateTime.parse(v).toZonedDateTime
    }
  )


  def parseJson[T](json: String)(implicit reader: Reader[T]): Either[Result, T] = {
    try {
      Right(read[T](json))
    } catch {
      case Invalid.Data(data, msg) => Left(Results.BadRequest(write(ErrorDescription("InvalidData", s"$msg: ${data.toString()}"))))
      case Invalid.Json(msg, input) => Left(Results.BadRequest(write(ErrorDescription("InvalidJson", s"$msg: $input"))))
      case e: Throwable => Left(Results.BadRequest(write(ErrorDescription("InvalidJson", s"${e.getClass.getName}: ${e.getMessage}"))))
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