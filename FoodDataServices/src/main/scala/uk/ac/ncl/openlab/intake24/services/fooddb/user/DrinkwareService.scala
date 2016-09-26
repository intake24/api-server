package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

trait DrinkwareService {

  def getDrinkwareSet(id: String): Either[LookupError, DrinkwareSet]
}
