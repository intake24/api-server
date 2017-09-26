package security.captcha

import scala.concurrent.Future

trait AsyncCaptchaService {
  def verify(response: String, remoteip: String): Future[Option[Boolean]]
}
