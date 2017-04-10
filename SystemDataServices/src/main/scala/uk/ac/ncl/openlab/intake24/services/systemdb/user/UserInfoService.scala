package uk.ac.ncl.openlab.intake24.services.systemdb.user

import uk.ac.ncl.openlab.intake24.errors.{ConstraintError, LookupError}

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */

case class UserInfoOut(userId: Long, firstName: Option[String], sex: Option[String],
                       yearOfBirth: Option[Int], weight: Option[Double], height: Option[Double],
                       levelOfPhysicalActivity: Option[Long])

case class UserInfoIn(firstName: Option[String], sex: Option[String], yearOfBirth: Option[Int],
                      weight: Option[Double], height: Option[Double], levelOfPhysicalActivityId: Option[Long])

trait UserInfoService {

  def update(userId: Long, userInfo: UserInfoIn): Either[ConstraintError, UserInfoOut]

  def get(userId: Long): Either[LookupError, UserInfoOut]

}
