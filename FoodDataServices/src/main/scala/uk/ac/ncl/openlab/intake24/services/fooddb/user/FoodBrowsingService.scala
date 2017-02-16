package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleError, LookupError}
import uk.ac.ncl.openlab.intake24.{UserCategoryContents, UserCategoryHeader}

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

  def getRootCategories(locale: String): Either[LocaleError, Seq[UserCategoryHeader]]

  def getCategoryContents(code: String, locale: String): Either[LocalLookupError, UserCategoryContents]

  /* The following methods are needed associated foods logic */
  def getFoodAllCategories(code: String): Either[LookupError, Set[String]]

  def getCategoryAllCategories(code: String): Either[LookupError, Set[String]]
}
