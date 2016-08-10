package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.UserCategoryContents
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodDataError
import uk.ac.ncl.openlab.intake24.UserFoodData
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.AssociatedFood

sealed trait SourceRecord

object SourceRecord {
  case class FoodRecord(code: String) extends SourceRecord
  case class CategoryRecord(code: String) extends SourceRecord
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
  case class Fallback(locale: String) extends SourceLocale
}

case class InheritableAttributeSources(sameAsBeforeOptionSource: InheritableAttributeSource, readyMealOptionSource: InheritableAttributeSource, reasonableAmountSource: InheritableAttributeSource)

case class FoodDataSources(localDescriptionSource: SourceLocale, nutrientTablesSource: SourceLocale, portionSizeSource: (SourceLocale, SourceRecord), inheritableAttributesSources: InheritableAttributeSources)

trait FoodBrowsingService {
  
  def rootCategories(locale: String): Either[DatabaseError, Seq[UserCategoryHeader]]

  def categoryContents(code: String, locale: String): Either[FoodCodeError, UserCategoryContents]

  /* The following methods are needed associated foods logic */
  def foodAllCategories(code: String): Either[FoodCodeError, Seq[String]]
  
  def categoryAllCategories(code: String): Either[FoodCodeError, Seq[String]]
}
