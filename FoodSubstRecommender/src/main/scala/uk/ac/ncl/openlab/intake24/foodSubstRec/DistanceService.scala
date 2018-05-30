package uk.ac.ncl.openlab.intake24.foodSubstRec

/**
  * Created by Tim Osadchiy on 25/04/2018.
  */
trait DistanceService {

  def getClosest(foodCode: String): Map[String, Double]

}
