package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.errors._

trait LocalesAdminService {
  def listLocales(): Either[UnexpectedDatabaseError, Map[String, Locale]]
  def getLocale(id: String): Either[LookupError, Locale]
  def createLocale(data: Locale): Either[CreateError, Unit]
  def updateLocale(id: String, data: Locale): Either[UpdateError, Unit]
  def deleteLocale(id: String): Either[DeleteError, Unit]
  def isTranslationRequired(id: String): Either[LookupError, Boolean]
}
