package parsers

import io.circe.Decoder
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results.BadRequest
import play.api.mvc.{MultipartFormData, Result}
import io.circe.generic.auto._

trait FormDataUtil extends JsonUtils {

  def getFile(key: String, parsedBody: MultipartFormData[TemporaryFile]): Either[Result, FilePart[TemporaryFile]] = {
    parsedBody.file(key) match {
      case Some(file) => Right(file)
      case None => Left(BadRequest(s"""{"cause":"File part "$key" is missing from the form data"}"""))
    }
  }

  def getMultipleData(key: String, parsedBody: MultipartFormData[TemporaryFile]): Either[Result, Seq[String]] = {
    parsedBody.dataParts.get(key) match {
      case Some(data) => Right(data)
      case None => Left(BadRequest(s"""{"cause":"Field "$key" is missing from the form data"}"""))
    }
  }

  def getOptionalMultipleData(key: String, parsedBody: MultipartFormData[TemporaryFile]): Either[Result, Seq[String]] = {
    parsedBody.dataParts.get(key) match {
      case Some(data) => Right(data)
      case None => Right(Seq())
    }
  }

  def getMultipleParsedData[T](key: String, parsedBody: MultipartFormData[TemporaryFile])(implicit reader: Decoder[T]): Either[Result, Seq[T]] =
    getMultipleData(key, parsedBody).right.flatMap {
      fields =>
        fields.map(parseJson[T](_)).foldRight(Right(Nil): Either[Result, List[T]]) {
          (result, acc) =>
            for (
              result <- result.right;
              acc <- acc.right
            ) yield result :: acc
        }
    }

  def getData(key: String, parsedBody: MultipartFormData[TemporaryFile]): Either[Result, String] = getMultipleData(key, parsedBody) match {
    case Left(e) => Left(e)
    case Right(seq) =>
      val size = seq.size
      if (size != 1)
        Left(BadRequest(s"""{"cause":"Expected one value for "$key", got $size"}"""))
      else
        Right(seq.head)
  }

  def getParsedData[T](key: String, parsedBody: MultipartFormData[TemporaryFile])(implicit reader: Decoder[T]): Either[Result, T] =
    getData(key, parsedBody).right.flatMap {
      str =>
        parseJson[T](str)
    }

  def getIntData(key: String, parsedBody: MultipartFormData[TemporaryFile]): Either[Result, Int] = getData(key, parsedBody).flatMap {
    str =>
      try {
        Right(Integer.parseInt(str))
      } catch {
        case e: RuntimeException => Left(BadRequest(s"""{"cause":"Expected "$key" to be an integer value, but "$str" is not}"""))
      }
  }
}
