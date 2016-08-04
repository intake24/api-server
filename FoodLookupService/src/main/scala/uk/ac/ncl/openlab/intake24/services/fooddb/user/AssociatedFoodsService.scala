package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CodeError
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood

trait AssociatedFoodsService {

  def associatedFoods(foodCode: String, locale: String): Either[CodeError, Seq[AssociatedFood]]
}
