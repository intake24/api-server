package controllers.system.asynchronous

import javax.inject.{Inject, Named, Singleton}

import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.mailer.{Email, MailerClient}
import uk.ac.ncl.openlab.intake24.errors.{AnyError, ErrorUtils}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{UserAdminService, UserProfile}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailSender @Inject()(configuration: Configuration,
                            userAdminService: UserAdminService,
                            mailerClient: MailerClient,
                            @Named("intake24") implicit val executionContext: ExecutionContext) {

  val logger = LoggerFactory.getLogger(classOf[EmailSender])

  private def sendTo(address: String, subject: String, from: String, messageBody: String, html: Boolean): Either[AnyError, Unit] =
    ErrorUtils.catchAll {
      val message =
        if (html)
          Email(subject, from, Seq(address), None, Some(messageBody))
        else
          Email(subject, from, Seq(address), Some(messageBody))
      mailerClient.send(message)
    }

  private def sendIfAllowed(userProfile: UserProfile, subject: String, from: String, messageBody: UserProfile => String, html: Boolean): Either[AnyError, Unit] =
    userProfile.email match {
      case Some(address) =>
        if (userProfile.emailNotifications) {
          logger.debug(s"Sending '$subject' to <$address>")
          sendTo(address, subject, from, messageBody(userProfile), html)
        } else {
          logger.warn(s"User ${userProfile.id} has e-mail notifications disabled, cannot send message")
          Right(())
        }
      case None =>
        logger.warn(s"User ${userProfile.id} has no e-mail address, cannot send message")
        Right(())
    }

  def send(userId: Long, subject: String, from: String, body: UserProfile => String): Future[Either[AnyError, Unit]] =
    Future {
      for (profile <- userAdminService.getUserById(userId);
           _ <- sendIfAllowed(profile, subject, from, body, false))
        yield ()
    }

  def sendHtml(userId: Long, subject: String, from: String, body: UserProfile => String): Future[Either[AnyError, Unit]] =
    Future {
      for (profile <- userAdminService.getUserById(userId);
           _ <- sendIfAllowed(profile, subject, from, body, true))
        yield ()
    }

  def send(address: String, subject: String, from: String, body: String): Future[Either[AnyError, Unit]] =
    Future {
      sendTo(address, subject, from, body, false)
    }

  def sendHtml(address: String, subject: String, from: String, body: String): Future[Either[AnyError, Unit]] =
    Future {
      sendTo(address, subject, from, body, true)
    }
}
