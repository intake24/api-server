package uk.ac.ncl.openlab.intake24.services.systemdb.user

import uk.ac.ncl.openlab.intake24.errors._

trait FoodPopularityService {

  def getPopularityCount(foodCodes: Seq[String]): Either[UnexpectedDatabaseError, Map[String, Int]]

  def incrementPopularityCount(foodCodes: Seq[String]): Either[UnexpectedDatabaseError, Unit]
}
