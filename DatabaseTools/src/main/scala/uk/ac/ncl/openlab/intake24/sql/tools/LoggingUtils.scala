package uk.ac.ncl.openlab.intake24.sql.tools

object LoggingUtils {
  def applySimpleLoggerSettings() = {
    System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true")
    System.setProperty("org.slf4j.simpleLogger.showThreadName", "false")
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd/MM/yyyy HH:mm:ss")
  }
}