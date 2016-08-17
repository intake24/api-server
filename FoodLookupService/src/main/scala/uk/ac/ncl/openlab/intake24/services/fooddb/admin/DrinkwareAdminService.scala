package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.DrinkwareHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService

trait DrinkwareAdminService extends DrinkwareService {

  def allDrinkwareSets(): Either[DatabaseError, Seq[DrinkwareHeader]]
  
  def deleteAllDrinkwareSets(): Either[DatabaseError, Unit]
  
  def createDrinkwareSets(sets: Seq[DrinkwareSet]): Either[DatabaseError, Unit]
}
