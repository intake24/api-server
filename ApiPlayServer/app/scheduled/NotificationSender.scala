package scheduled

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.mailer.{Email, MailerClient}
import sms.SMSService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{UserAdminService, UserProfile}
import uk.ac.ncl.openlab.intake24.services.systemdb.notifications.{Notification, NotificationScheduleDataService}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{PublicSurveyParameters, SurveyService}
import urlShort.ShortUrlService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Tim Osadchiy on 12/10/2017.
  */
trait NotificationSender

@Singleton
class NotificationSenderImpl @Inject()(system: ActorSystem,
                                       notificationDataService: NotificationScheduleDataService,
                                       shortUrlService: ShortUrlService,
                                       userService: UserAdminService,
                                       smsService: SMSService,
                                       mailerClient: MailerClient,
                                       surveyService: SurveyService,
                                       configuration: Configuration,
                                       implicit val executionContext: ExecutionContext) extends NotificationSender {

  system.scheduler.schedule(0.second, 2.minutes) {
    val logger = LoggerFactory.getLogger("NotificationSenderImpl")

    sendNotifications()

    def sendNotifications() = {
      notificationDataService.list().foreach {
        notificationList =>
          val now = ZonedDateTime.now()
          val relevantNotifications = notificationList.filter(n => now.isAfter(n.dateTime))
          sendNotificationsRecursive(relevantNotifications)
      }
    }

    def sendNotificationsRecursive(notList: Seq[Notification]): Unit = notList.headOption.foreach {
      notification =>
        sendSingleNotification(notification)
          .foreach { _ =>
            notificationDataService.clean(notification.id)
            sendNotificationsRecursive(notList.drop(1))
          }
    }

    def sendSingleNotification(notification: Notification): Future[Boolean] = notification.surveyId.map { surveyId =>
      surveyService.getPublicSurveyParameters(surveyId) match {
        case Right(surveyProfile) =>
          userService.getUserById(notification.userId) match {
            case Right(userProfile) => sendNotificationToUser(surveyId, surveyProfile, userProfile)
            case Left(e) => {
              notifyAdminUserNotFound(notification.userId, surveyProfile.supportEmail)
              Future(false)
            }
          }
        case Left(e) => Future(false)
      }
    }.getOrElse(Future(false))


    def sendNotificationToUser(surveyId: String, surveyProfile: PublicSurveyParameters, userProfile: UserProfile): Future[Boolean] =
      userService.getAuthTokenByUserId(userProfile.id) match {
        case Right(token) => {
          getLoginMessage(surveyId, token).map { loginMessage =>
            sendNotificationEmailToUser(userProfile, surveyProfile, loginMessage)
            sendNotificationSmsToUser(userProfile, surveyProfile, loginMessage)
            if (userProfile.phone.isDefined || userProfile.email.isDefined) {
              notifyAdminSuccessful(userProfile, surveyProfile.supportEmail)
            }
            true
          }
        }
        case Left(e) => {
          notifyAdminNoToken(userProfile, surveyProfile.supportEmail)
          Future(false)
        }
      }

    def sendNotificationEmailToUser(userProfile: UserProfile, surveyProfile: PublicSurveyParameters, message: String) =
      userProfile.email match {
        case Some(email) => sendEmail(email, surveyProfile.supportEmail, "Itake24. Time for dietary recall", message)
        case None => notifyAdminNoEmail(userProfile, surveyProfile.supportEmail)
      }

    def sendNotificationSmsToUser(userProfile: UserProfile, surveyProfile: PublicSurveyParameters, message: String) =
      userProfile.phone match {
        case Some(phone) => smsService.sendMessage(message, phone)
        case None => notifyAdminNoPhone(userProfile, surveyProfile.supportEmail)
      }

    def getLoginMessage(surveyId: String, token: String): Future[String] = {
      val authUrl = produceAuthUrl(surveyId, token)
      shortUrlService.shorten(authUrl).map(shUrl => produceMessage(shUrl))
    }

    def produceMessage(url: String) =
      s"Intake24. Please use your selected device to record your dietary intake $url"

    def produceAuthUrl(surveyId: String, authToken: String) =
      s"${configuration.get[String]("intake24.surveyFrontendUrl")}/surveys/$surveyId?auth=$authToken"

    def notifyAdminUserNotFound(userId: Long, email: String) = {
      val message = s"Couldn't notify participant. User with Id $userId not found."
      sendNotificationFailedEmailToAdmin(email, message)
    }

    def notifyAdminSuccessful(userProfile: UserProfile, email: String) = {
      val message =
        s"""
           |Participant notified by: ${if (userProfile.email.isDefined) "email" else "NOT email"}, ${if (userProfile.phone.isDefined) "phone" else "NOT phone"}.
           |User Id: ${userProfile.id}, Name: ${userProfile.name.getOrElse("Not known")}.
           """.stripMargin
      sendEmail(email, email, "Intake24. Successfully notified", message)
    }

    def notifyAdminNoEmail(userProfile: UserProfile, email: String) = {
      val message =
        s"""
           |Couldn't notify participant.
           |No email for user Id: ${userProfile.id}, Name: ${userProfile.name.getOrElse("Not known")}.
           """.stripMargin
      sendNotificationFailedEmailToAdmin(email, message)
    }

    def notifyAdminNoToken(userProfile: UserProfile, email: String) = {
      val message =
        s"""
           |Couldn't notify participant.
           |No token for user Id: ${userProfile.id}, Name: ${userProfile.name.getOrElse("Not known")}.
           """.stripMargin
      sendNotificationFailedEmailToAdmin(email, message)
    }

    def notifyAdminNoPhone(userProfile: UserProfile, email: String) = {
      val message =
        s"""
           |Couldn't notify participant.
           |No phone for user Id: ${userProfile.id}, Name: ${userProfile.name.getOrElse("Not known")}.
           """.stripMargin
      sendNotificationFailedEmailToAdmin(email, message)
    }

    def sendNotificationFailedEmailToAdmin(email: String, message: String) =
      sendEmail(email, email, "Intake24. Failed to notify participant", message)

    def sendEmail(emailTo: String, emailFrom: String, title: String, message: String) = {
      val email = Email(title, s"Intake24 <$emailFrom>", Seq(emailTo), Some(message))
      mailerClient.send(email)
    }
  }


}
