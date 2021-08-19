package scheduled.notificationSender

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.mailer.{Email, MailerClient}
import sms.SMSService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{UserAdminService, UserProfile}
import uk.ac.ncl.openlab.intake24.services.systemdb.notifications.{NewNotification, Notification, NotificationScheduleDataService}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{PublicSurveyParameters, SurveyService}
import uk.ac.ncl.openlab.intake24.services.systemdb.uxEvents.UxEventsDataService
import uk.ac.ncl.openlab.intake24.shorturls.{ShortUrlsHttpClient, ShortUrlsRequest}

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Tim Osadchiy on 12/10/2017.
 */
trait NotificationSender

@Singleton
class NotificationSenderImpl @Inject()(system: ActorSystem,
                                       notificationDataService: NotificationScheduleDataService,
                                       shortUrlsService: ShortUrlsHttpClient,
                                       userService: UserAdminService,
                                       smsService: SMSService,
                                       mailerClient: MailerClient,
                                       surveyService: SurveyService,
                                       uxEventsDataService: UxEventsDataService,
                                       configuration: Configuration,
                                       implicit val executionContext: ExecutionContext) extends NotificationSender {

  val logger = LoggerFactory.getLogger(classOf[NotificationSenderImpl])

  system.scheduler.scheduleWithFixedDelay(0.second, 2.minutes)(new Runnable {

    val NOTIFY_AGAIN_AFTER_MINUTES = 40
    val ADMIN_NAME = "Tim"

    override def run(): Unit = sendNotifications()

    case class MessagePack(email: String, sms: String)

    def sendNotifications() = {
      notificationDataService.list() match {
        case Right(notificationList) =>
          val now = ZonedDateTime.now()
          val relevantNotifications = notificationList.filter(n => now.isAfter(n.dateTime))
          sendNotificationsRecursive(relevantNotifications)
        case Left(e) => logError(s"Failed to retreive notification list: ${e.exception.getMessage}")
      }
    }

    def sendNotificationsRecursive(notList: Seq[Notification]): Unit = notList.headOption.foreach {
      notification => {
        val callback = () => {
          notificationDataService.clean(notification.id).left.foreach(e =>
            logError(s"Failed to delete notification with DB Error: ${e.exception.getMessage}"))
          sendNotificationsRecursive(notList.drop(1))
        }
        if (notificationShouldBeSent(notification)) {
          sendSingleNotification(notification).foreach { positiveResult =>
            if (positiveResult) {
              createReminder(notification)
            }
            callback()
          }
        } else {
          callback()
        }
      }
    }

    def sendSingleNotification(notification: Notification): Future[Boolean] = notification.surveyId.map { surveyId =>
      surveyService.getPublicSurveyParameters(surveyId) match {
        case Right(surveyProfile) =>
          userService.getUserById(notification.userId) match {
            case Right(userProfile) => sendNotificationToUser(notification, surveyId, surveyProfile, userProfile)
            case Left(e) => {
              logError(s"Couldn't find user: ${notification.userId}")
              notifyAdminUserNotFound(notification.userId, surveyProfile.supportEmail)
              Future(false)
            }
          }
        case Left(e) => {
          logError(s"Couldn't find parameters for survey id: $surveyId")
          Future(false)
        }
      }
    }.getOrElse(Future(false))


    def sendNotificationToUser(notification: Notification, surveyId: String, surveyProfile: PublicSurveyParameters, userProfile: UserProfile): Future[Boolean] =
      userService.getAuthTokenByUserId(userProfile.id) match {
        case Right(token) =>
          getLoginMessage(notification, userProfile, surveyId, token).map {
            case Some(msgPack) =>
              sendNotificationEmailToUser(userProfile, surveyProfile, msgPack.email)
              sendNotificationSmsToUser(userProfile, surveyProfile, msgPack.sms)
              if (userProfile.phone.isDefined || userProfile.email.isDefined) {
                notifyAdminSuccessful(userProfile, surveyProfile.supportEmail, msgPack.email)
              }
              true
            case None =>
              logError(s"Couldn't find notification with type: ${notification.notificationType}")
              false
          }
        case Left(e) => {
          logError(s"No auth token for user: ${userProfile.id}")
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

    def getLoginMessage(notification: Notification, userProfile: UserProfile, surveyId: String, token: String): Future[Option[MessagePack]] = {
      val authUrl = produceAuthUrl(surveyId, token)

      shortUrlsService.getShortUrls(ShortUrlsRequest(Seq(authUrl))).unsafeToFuture().map(shUrl => {
        for (
          emailMsg <- produceEmailMessage(notification, userProfile, shUrl.shortUrls.head);
          smsMsg <- produceSmsMessage(notification, userProfile, shUrl.shortUrls.head)
        ) yield MessagePack(emailMsg, smsMsg)
      })
    }

    def produceEmailMessage(notification: Notification, userProfile: UserProfile, url: String) =
      NotificationMessages.formatEmailMessage(notification.notificationType, ADMIN_NAME, userProfile.name.map(n => n.split(" ").head).getOrElse("Friend"), url)

    def produceSmsMessage(notification: Notification, userProfile: UserProfile, url: String) =
      NotificationMessages.formatSmsMessage(notification.notificationType, ADMIN_NAME, userProfile.name.map(n => n.split(" ").head).getOrElse("Friend"))

    def produceAuthUrl(surveyId: String, authToken: String) =
      s"${configuration.get[String]("intake24.surveyFrontendUrl")}/surveys/$surveyId?auth=$authToken"

    def notifyAdminUserNotFound(userId: Long, email: String) = {
      val message = s"Couldn't notify participant. User with Id $userId not found."
      sendNotificationFailedEmailToAdmin(email, message)
    }

    def notifyAdminSuccessful(userProfile: UserProfile, email: String, notificationText: String) = {
      val message =
        s"""
           |Participant notified by: ${if (userProfile.email.isDefined) "email" else "NOT email"}, ${if (userProfile.phone.isDefined) "phone" else "NOT phone"}.
           |User Id: ${userProfile.id}, Name: ${userProfile.name.getOrElse("Not known")}.
           |Email notification text:
           |$notificationText
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

    def logError(msg: String) = logger.error(s"${getClass.getSimpleName}. $msg")

    def createReminder(notification: Notification) = {
      val newDate = ZonedDateTime.now().plusMinutes(NOTIFY_AGAIN_AFTER_MINUTES)
      val newNotification: Option[NewNotification] = notification match {
        case n: Notification if n.notificationType == Notification.NotificationTypeLoginSecondReminder => None
        case n: Notification if n.notificationType == Notification.NotificationTypeLoginFirstReminder =>
          Some(NewNotification(notification.userId, notification.surveyId, newDate, Notification.NotificationTypeLoginSecondReminder))
        case n: Notification if n.notificationType != Notification.NotificationTypeLoginFirstReminder =>
          Some(NewNotification(notification.userId, notification.surveyId, newDate, Notification.NotificationTypeLoginFirstReminder))
      }
      newNotification.foreach(n => notificationDataService.create(n))
    }

    def notificationShouldBeSent(notification: Notification) =
      !Seq(Notification.NotificationTypeLoginFirstReminder,
        Notification.NotificationTypeLoginSecondReminder,
        Notification.NotificationTypeLoginLast).contains(notification.notificationType) ||
        shouldRemind(notification) || shouldSendLastNotification(notification)

    def shouldRemind(notification: Notification): Boolean = notification.surveyId.exists { surveyId =>

      /**
       * This function checks if user was active within the last 2 hours.
       * Used to check if previous notification was ignored
       */
      if (!Seq(Notification.NotificationTypeLoginSecondReminder, Notification.NotificationTypeLoginFirstReminder).contains(notification.notificationType)) {
        false
      } else {
        val dateFrom = notification.dateTime.minusHours(2)
        val userSubmitted = surveyService
          .userSubmittedWithinPeriod(surveyId, notification.userId, dateFrom, ZonedDateTime.now())
          .getOrElse(false)
        val userWasActive = uxEventsDataService
          .userWasActiveWithinPeriod(notification.userId, dateFrom, ZonedDateTime.now())
          .getOrElse(false)
        !userSubmitted && !userWasActive
      }
    }

    def shouldSendLastNotification(notification: Notification): Boolean = notification.surveyId.exists { surveyId =>

      /**
       * This function checks if user submitted within the last 18 hours.
       * Used to check if there is a need to ask user for submission on the last morning
       */
      val dateFrom = notification.dateTime.minusHours(18)
      val userSubmitted = surveyService
        .userSubmittedWithinPeriod(surveyId, notification.userId, dateFrom, ZonedDateTime.now())
        .getOrElse(false)
      notification.notificationType == Notification.NotificationTypeLoginLast && !userSubmitted
    }

  })
}
