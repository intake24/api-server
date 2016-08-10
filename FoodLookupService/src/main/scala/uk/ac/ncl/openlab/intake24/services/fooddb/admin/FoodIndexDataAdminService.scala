package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.SplitList
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait FoodIndexDataAdminService {
  def synsets(locale: String): Either[LocaleError, Seq[Set[String]]]
  def splitList(locale: String): Either[LocaleError, SplitList]
  
  def deleteSynsets(locale: String): Either[DatabaseError, Unit]
  def deleteSplitList(locale: String): Either[DatabaseError, Unit]
  
  def createSynsets(synsets: Seq[Set[String]], locale: String): Either[DatabaseError, Unit]
  def createSplitList(splitList: SplitList, locale: String): Either[DatabaseError, Unit]
}