package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.{DrinkwareHeader, DrinkwareSet}
import uk.ac.ncl.openlab.intake24.errors.{CreateError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService

trait DrinkwareAdminService extends DrinkwareService {

  def listDrinkwareSets(): Either[UnexpectedDatabaseError, Map[String, DrinkwareHeader]]
  
  def deleteAllDrinkwareSets(): Either[UnexpectedDatabaseError, Unit]
  
  def createDrinkwareSets(sets: Seq[DrinkwareSet]): Either[CreateError, Unit]
}
