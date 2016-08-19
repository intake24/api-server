package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.DrinkwareHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError

trait DrinkwareAdminService extends DrinkwareService {

  def listDrinkwareSets(): Either[DatabaseError, Map[String, DrinkwareHeader]]
  
  def deleteAllDrinkwareSets(): Either[DatabaseError, Unit]
  
  def createDrinkwareSets(sets: Seq[DrinkwareSet]): Either[CreateError, Unit]
}
