package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors.{CreateError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService
import uk.ac.ncl.openlab.intake24.{DrinkwareHeader, DrinkwareSet, DrinkwareSetRecord}

trait DrinkwareAdminService extends DrinkwareService {

  def listDrinkwareSets(): Either[UnexpectedDatabaseError, Map[String, DrinkwareHeader]]

  def deleteAllDrinkwareSets(): Either[UnexpectedDatabaseError, Unit]

  def createDrinkwareSets(sets: Seq[DrinkwareSet]): Either[CreateError, Unit]

  def createDrinkwareSetRecord(record: DrinkwareSetRecord): Either[CreateError, Unit]
}
