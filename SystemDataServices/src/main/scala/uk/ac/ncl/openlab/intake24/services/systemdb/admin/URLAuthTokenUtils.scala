package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.security.SecureRandom
import java.util.Base64

import scala.util.Random

object URLAuthTokenUtils {
  private val secureRandom = new SecureRandom()
  private val random = new Random()

  private val base64Encoder = Base64.getUrlEncoder()

  def generateToken = {
    val bytes = new Array[Byte](24)
    secureRandom.nextBytes(bytes)
    base64Encoder.encodeToString(bytes)
  }

  private val shortUrlAlphabet = "0123456789abcdefghijkmnpqrstuvwxyz" // l and o removed intentionally to prevent confusion with 1 and 0

  def generateShortToken(length: Int): String = {
    val sb = new StringBuffer()

    1.to(length).foreach {
      _ =>
        sb.append(shortUrlAlphabet.charAt(random.nextInt(shortUrlAlphabet.length)))
    }

    sb.toString
  }
}
