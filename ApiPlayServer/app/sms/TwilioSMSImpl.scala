package sms

import com.google.inject.{Inject, Singleton}
import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message
import play.api.{Configuration, Logger}

@Singleton
class TwilioSMSImpl @Inject()(config: Configuration) extends SMSService {

  val accountSid = config.getString("twilio.accountSid")
  val authToken = config.getString("twilio.authToken")
  val fromNumber = config.getString("twilio.fromNumber")

  val mockSetting = config.getString("twilio.mock")

  val mock = mockSetting.isDefined || {
    if (accountSid.isDefined && authToken.isDefined && fromNumber.isDefined)
      false
    else {
      Logger.warn("Twilio configuration missing, falling back to mock implementation")
      true
    }
  }

  if (!mock)
    Twilio.init(accountSid.get, authToken.get)

  def sendMessage(messageBody: String, to: String): Unit = {

    if (mock) {
      val reason = if (mockSetting.isDefined) "twilio.mock setting is set" else "Twilio configuration is missing"
      Logger.info(s"""Sending mock SMS message "$messageBody" to $to. Twilio SMS service disabled because $reason.""")
    } else {
      val message = Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber.get), messageBody).create()
      Logger.debug(s"Twilio message sent, sid: ${message.getSid()}")
    }
  }
}
