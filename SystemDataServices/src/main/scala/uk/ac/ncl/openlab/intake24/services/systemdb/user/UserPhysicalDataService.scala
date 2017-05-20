package uk.ac.ncl.openlab.intake24.services.systemdb.user

import java.time.{LocalDate, ZonedDateTime}

import uk.ac.ncl.openlab.intake24.errors.{ConstraintError, LookupError}

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */

case class UserPhysicalDataOut(userId: Long, sex: Option[String],
                               birthdate: Option[LocalDate], weight: Option[Double], height: Option[Double],
                               physicalActivityLevelId: Option[Long])

case class UserPhysicalDataIn(sex: Option[String],
                              birthdate: Option[LocalDate], weight: Option[Double],
                              height: Option[Double], physicalActivityLevelId: Option[Long])

trait UserPhysicalDataService {

  def update(userId: Long, userInfo: UserPhysicalDataIn): Either[ConstraintError, UserPhysicalDataOut]

  def get(userId: Long): Either[LookupError, UserPhysicalDataOut]
}
