package uk.ac.ncl.openlab.intake24.services.systemdb.user

import uk.ac.ncl.openlab.intake24.errors.{ConstraintError, LookupError}

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */

case class UserPhysicalDataOut(userId: Long, firstName: Option[String], sex: Option[String],
                               yearOfBirth: Option[Int], weight: Option[Double], height: Option[Double],
                               levelOfPhysicalActivityId: Option[Long])

case class UserPhysicalDataIn(firstName: Option[String], sex: Option[String], yearOfBirth: Option[Int],
                              weight: Option[Double], height: Option[Double], levelOfPhysicalActivityId: Option[Long])

trait UserPhysicalDataService {

  def update(userId: Long, userInfo: UserPhysicalDataIn): Either[ConstraintError, UserPhysicalDataOut]

  def get(userId: Long): Either[LookupError, UserPhysicalDataOut]
}
