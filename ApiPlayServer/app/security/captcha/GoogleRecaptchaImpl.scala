package security.captcha

import javax.inject.{Inject, Singleton}

import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

import io.circe._
import io.circe.parser._

@Singleton
class GoogleRecaptchaImpl @Inject()(ws: WSClient,
                                    configuration: Configuration,
                                    executionContext: ExecutionContext) extends AsyncCaptchaService {

  private val logger = LoggerFactory.getLogger(classOf[GoogleRecaptchaImpl])

  private val secretKey = configuration.get[String]("intake24.recaptcha.secretKey")

  private implicit val implicitEC = executionContext

  private case class VerifyResponse(success: Boolean, errorCodes: Option[Seq[String]])

  private implicit val responseDecoder =
    Decoder.forProduct2[VerifyResponse, Boolean, Option[Seq[String]]]("success", "error-codes") {
      (success, errorCodes) =>
        VerifyResponse(success, errorCodes)
    }

  def verify(response: String, remoteip: String) = {
    logger.debug("Sending Recaptcha verify request:")
    logger.debug(s"response=$response")
    logger.debug(s"secret=${secretKey.substring(0, 8)}...")
    ws.url("https://www.google.com/recaptcha/api/siteverify")
      .withQueryStringParameters("secret" -> secretKey, "response" -> response)
      .post("")
      .map {
        response =>
          response.status match {
            case 200 =>

              decode[VerifyResponse](response.body) match {
                case Right(response) =>
                  if (!response.success)
                    logger.debug(s"Captcha failed: " + response.errorCodes.getOrElse(Seq()).mkString(", "))
                  Some(response.success)
                case Left(error) =>
                  logger.error("Failed to parse Recaptcha verify response", error)
                  None
              }
            case status =>
              logger.error(s"Recaptcha verify failed with HTTP status $status:")
              logger.error(response.body)
              None

          }
      }
  }
}
