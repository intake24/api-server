package scheduled.notificationSender

import uk.ac.ncl.openlab.intake24.services.systemdb.notifications.Notification

/**
  * Created by Tim Osadchiy on 16/03/2018.
  */
object NotificationMessages {

  val EMAIL_SUFFIX = "Please use the same browser that you used for the previous recalls. Follow this url to login: %s \n\n -%s from Intake24"
  val EMAIL_SHORT_SUFFIX = "Follow this url to login: %s \\n\\n -%s from Intake24"
  val MOBILE_SUFFIX = "Login url was sent to your email. -%s from Intake24"

  val emailMessages = Map(
    Notification.NotificationTypeLoginRecallForYesterday ->
      s"Morning %s 👋 It's time to record your diet for YESTERDAY 😋 If this isn't your first recall during this study, please use the same browser that you used for the previous recall. $EMAIL_SHORT_SUFFIX",
    Notification.NotificationTypeLoginRecallForToday ->
      s"Morning %s ☀️ Today you should record your diet for TODAY as the day goes on. $EMAIL_SUFFIX",
    Notification.NotificationTypeLoginBackToRecall ->
      s"Hi %s 👋 It's time to continue recording your diet 🙂 $EMAIL_SUFFIX",
    Notification.NotificationTypeLoginLast ->
      s"Morning %s ☀️ Please don't forget to submit your dietary recall that you started yesterday. $EMAIL_SUFFIX",
    Notification.NotificationTypeLoginFirstReminder ->
      s"Hi %s 😃 Please don't forget to record your diet 🍅 $EMAIL_SHORT_SUFFIX",
    Notification.NotificationTypeLoginSecondReminder ->
      s"Hi %s! Hope you didn't forget to record your diet 🙂 $EMAIL_SHORT_SUFFIX"
  )

  val mobileMessages = Map(
    Notification.NotificationTypeLoginRecallForYesterday ->
      s"Morning %s 👋 Please use your selected browser to record your diet for YESTERDAY 😋 $MOBILE_SUFFIX",
    Notification.NotificationTypeLoginRecallForToday ->
      s"Morning %s ☀️ Today you should record your diet for TODAY as it goes on. $MOBILE_SUFFIX",
    Notification.NotificationTypeLoginBackToRecall ->
      s"Hi %s 👋 It's time to continue recording your diet 🙂 $MOBILE_SUFFIX",
    Notification.NotificationTypeLoginLast ->
      s"Morning %s ☀️ Please don't forget to submit your dietary recall that you started  yesterday. $MOBILE_SUFFIX",
    Notification.NotificationTypeLoginFirstReminder ->
      s"Hi %s 😃 Please don't forget to record your diet 🍅 $MOBILE_SUFFIX",
    Notification.NotificationTypeLoginSecondReminder ->
      s"Hi %s! Hope you didn't forget to record your diet 🙂 $MOBILE_SUFFIX"
  )

  def formatEmailMessage(messageTemplateId: String, from: String, to: String, url: String) =
    emailMessages.get(messageTemplateId).map(_.format(to, url, from))

  def formatSmsMessage(messageTemplateId: String, from: String, to: String) =
    mobileMessages.get(messageTemplateId).map(_.format(to, from))

}
