package sms

trait SMSService {
  def sendMessage(message: String, to: String): Unit
}
