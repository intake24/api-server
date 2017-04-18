package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.security.SecureRandom
import java.util.Base64

object URLAuthTokenUtils {
  private val secureRandom = new SecureRandom()
  private val base64Encoder = Base64.getUrlEncoder()

  def generateToken = {
    val bytes = new Array[Byte](24)
    secureRandom.nextBytes(bytes)
    base64Encoder.encodeToString(bytes)
  }
}
