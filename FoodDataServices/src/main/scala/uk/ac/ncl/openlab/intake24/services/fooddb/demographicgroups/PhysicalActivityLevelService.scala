package uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError

/**
  * Created by Tim Osadchiy on 18/05/2017.
  */

case class PhysicalActivityLevelOut(id: Long, name: String, coefficient: Double)

trait PhysicalActivityLevelService {

  def list(): Either[UnexpectedDatabaseError, Seq[PhysicalActivityLevelOut]]

}
