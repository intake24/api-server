package sms

import com.google.inject.{Inject, Singleton}
import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message
import org.slf4j.LoggerFactory
import play.api.Configuration

@Singleton
class TwilioSMSImpl @Inject()(config: Configuration) extends SMSService {

  val accountSid = config.getOptional[String]("twilio.accountSid")
  val authToken = config.getOptional[String]("twilio.authToken")
  val fromNumber = config.getOptional[String]("twilio.fromNumber")

  val mockSetting = config.getOptional[String]("twilio.mock")

  val logger = LoggerFactory.getLogger(classOf[TwilioSMSImpl])

  val mock = mockSetting.isDefined || {
    if (accountSid.isDefined && authToken.isDefined && fromNumber.isDefined)
      false
    else {
      logger.warn("Twilio configuration missing, falling back to mock implementation")
      true
    }
  }

  if (!mock)
    Twilio.init(accountSid.get, authToken.get)

  def sendMessage(messageBody: String, to: String): Unit = {

    if (mock) {
      val reason = if (mockSetting.isDefined) "twilio.mock setting is set" else "Twilio configuration is missing"
      logger.info(s"""Sending mock SMS message "$messageBody" to $to. Twilio SMS service disabled because $reason.""")
    } else {
      val message = Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber.get), messageBody).create()
      logger.debug(s"Twilio message sent, sid: ${message.getSid()}")
    }
  }
}
