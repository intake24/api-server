package uk.ac.ncl.openlab.intake24.services

import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError

trait LocaleManagementService {
  def allLocales(): Either[DatabaseError, Seq[Locale]]
  def locale(id: String): Either[LocaleError, Locale]
  def createLocale(data: Locale): Either[DatabaseError, Unit]
  def updateLocale(id: String, data: Locale): Either[LocaleError, Unit]
  def deleteLocale(id: String): Either[LocaleError, Unit]
}
