package parsers

import java.time.{LocalDate, ZonedDateTime}
import java.time.format.DateTimeFormatter

import cats.syntax.either._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import play.api.http.ContentTypes
import play.api.mvc.{BodyParser, BodyParsers, Result, Results}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JsonUtils {

  import BodyParsers.parse._


  implicit val offsetDateTimeEncoder = new Encoder[ZonedDateTime] {
    def apply(a: ZonedDateTime): Json = Json.fromString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(a))
  }

  implicit val offsetDateTimeDecoder = Decoder.decodeString.emap {
    str =>
      Either.catchNonFatal(ZonedDateTime.parse(str)).leftMap { t => s"${t.getClass.getName}: ${t.getMessage}" }
  }

  implicit val dateEncoder = new Encoder[LocalDate] {
    def apply(a: LocalDate): Json = Json.fromString(DateTimeFormatter.ISO_DATE.format(a))
  }

  implicit val dateDecoder = Decoder.decodeString.emap {
    str =>
      Either.catchNonFatal(LocalDate.parse(str)).leftMap { t => s"${t.getClass.getName}: ${t.getMessage}" }
  }

  implicit def optionEncoder[T](implicit enc: Encoder[T]) = new Encoder[Option[T]] {
    def apply(a: Option[T]): Json = a match {
      case Some(v) => Json.arr(v.asJson)
      case None => Json.arr()
    }
  }

  implicit def optionDecoder[T](implicit dec: Decoder[T]) = new Decoder[Option[T]] {
    def apply(c: HCursor): Decoder.Result[Option[T]] =
      c.values match {
        case Some(values) => values match {
          case Vector(v) => dec.decodeJson(v).map(Some(_))
          case Vector() => Right(None)
          case _ => Left(DecodingFailure(s"Expected either 0 or 1 values in array, got ${values.size} ", c.history))
        }
        case None => Left(DecodingFailure("Expected a JSON array", c.history))
      }
  }

  implicit def eitherEncoder[L, R](implicit encL: Encoder[L], encR: Encoder[R]) = new Encoder[Either[L, R]] {
    def apply(a: Either[L, R]): Json = a match {
      case Left(lv) => Json.arr(Json.fromInt(0), lv.asJson)
      case Right(rv) => Json.arr(Json.fromInt(1), rv.asJson)
    }
  }

  implicit def eitherDecoder[L, R](implicit decL: Decoder[L], decR: Decoder[R]) = new Decoder[Either[L, R]] {
    def apply(c: HCursor): Decoder.Result[Either[L, R]] = c.values match {
      case Some(values) => values match {
        case Vector(t, v) =>
          for (t <- Decoder.decodeInt.decodeJson(t).right;
               v <- (t match {
                 case 0 => decL.decodeJson(v).map(Left(_))
                 case 1 => decR.decodeJson(v).map(Right(_))
                 case _ => Left(DecodingFailure("First value of the array must be 0 or 1", c.history))
               }).right) yield v
        case _ => Left(DecodingFailure(s"Expected either 0 or 1 values in array, got ${values.size} ", c.history))
      }
      case None => Left(DecodingFailure("Expected a JSON array", c.history))
    }
  }

  def toJsonString[T](v: T)(implicit enc: Encoder[T]) = v.asJson.noSpaces


  def parseJson[T](json: String)(implicit dec: Decoder[T]): Either[Result, T] =
    decode[T](json).leftMap {
      e => Results.BadRequest(ErrorDescription("InvalidJSON", e.toString).asJson.noSpaces)
    }

  def withParsedValue[T](json: String)(block: T => Result)(implicit dec: Decoder[T]): Result =
    parseJson[T](json) match {
      case Right(value) => block(value)
      case Left(errorResult) => errorResult
    }

  def jsonBodyParser[T](implicit dec: Decoder[T]): BodyParser[T] =
    when(
      request => request.contentType.exists(_.equalsIgnoreCase(ContentTypes.JSON)),
      raw,
      _ => Future.successful(Results.UnsupportedMediaType)
    ).validate {
      rawBuffer =>
        rawBuffer.asBytes() match {
          case Some(byteString) => parseJson[T](byteString.utf8String)
          case None => Left(Results.EntityTooLarge)
        }
    }
}