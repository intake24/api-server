package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError

trait DrinkwareService {

  def drinkwareSet(id: String): Either[ResourceError, DrinkwareSet]
}
