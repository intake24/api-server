package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError

trait AssociatedFoodsService {

  def getAssociatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFood]]
}
