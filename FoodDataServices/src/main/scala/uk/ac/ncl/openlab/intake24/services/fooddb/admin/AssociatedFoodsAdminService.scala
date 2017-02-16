package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleOrParentError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.{AssociatedFood, AssociatedFoodWithHeader}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AssociatedFoodsService

trait AssociatedFoodsAdminService extends AssociatedFoodsService {
  
  def getAssociatedFoodsWithHeaders(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]]
  def updateAssociatedFoods(foodCode: String, associatedFoods: Seq[AssociatedFood], locale: String): Either[LocaleOrParentError, Unit]
  
  def deleteAllAssociatedFoods(locale: String): Either[UnexpectedDatabaseError, Unit]
  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String): Either[LocaleOrParentError, Unit]
}
