package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalFoodCodeError

trait AssociatedFoodsService {

  def associatedFoods(foodCode: String, locale: String): Either[LocalFoodCodeError, Seq[AssociatedFood]]
}
