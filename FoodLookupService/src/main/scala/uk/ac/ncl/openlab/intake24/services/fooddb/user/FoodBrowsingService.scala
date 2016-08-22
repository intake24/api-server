package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.UserCategoryContents
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.UserFoodData
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

sealed trait SourceRecord

object SourceRecord {
  case class FoodRecord(code: String) extends SourceRecord
  case class CategoryRecord(code: String) extends SourceRecord
  case object NoRecord extends SourceRecord
}

sealed trait InheritableAttributeSource

object InheritableAttributeSource {
  case class FoodRecord(code: String) extends InheritableAttributeSource
  case class CategoryRecord(code: String) extends InheritableAttributeSource
  case object Default extends InheritableAttributeSource
}

sealed trait SourceLocale

object SourceLocale {
  case class Current(locale: String) extends SourceLocale
  case class Prototype(locale: String) extends SourceLocale
}

case class InheritableAttributeSources(sameAsBeforeOptionSource: InheritableAttributeSource, readyMealOptionSource: InheritableAttributeSource, reasonableAmountSource: InheritableAttributeSource)

case class FoodDataSources(localDescriptionSource: SourceLocale, nutrientTablesSource: SourceLocale, portionSizeSource: (SourceLocale, SourceRecord), inheritableAttributesSources: InheritableAttributeSources)

trait FoodBrowsingService {
  
  def rootCategories(locale: String): Either[LocaleError, Seq[UserCategoryHeader]]

  def categoryContents(code: String, locale: String): Either[LocalLookupError, UserCategoryContents]

  /* The following methods are needed associated foods logic */
  def foodAllCategories(code: String): Either[LookupError, Set[String]]
  
  def categoryAllCategories(code: String): Either[LookupError, Set[String]]
}
