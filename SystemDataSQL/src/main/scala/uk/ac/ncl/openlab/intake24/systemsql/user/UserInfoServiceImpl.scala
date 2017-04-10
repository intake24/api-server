package uk.ac.ncl.openlab.intake24.systemsql.user

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm.{Macro, Row, SQL, SimpleSql}
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{SurveyService, UserInfoIn, UserInfoOut, UserInfoService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */
class UserInfoServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends UserInfoService
  with SqlDataService with SqlResourceLoader {

  val constraintErrorsPartialFn = PartialFunction[String, PSQLException => ConstraintError] {
    constraintName => (e: PSQLException) => ConstraintViolation(e.toString, e)
  }

  private case class UserInfoRow(user_id: Long, first_name: Option[String], sex: Option[String],
                                 year_of_birth: Option[Int], weight_kg: Option[Double], height_cm: Option[Double],
                                 level_of_physical_activity_id: Option[Long]) {
    def toUserInfoOut(): UserInfoOut = {
      UserInfoOut(this.user_id, this.first_name, this.sex, this.year_of_birth, this.weight_kg,
        this.height_cm, this.level_of_physical_activity_id)
    }
  }

  private object UserInfoRow {
    def getSqlUpdate(userId: Long, userInfo: UserInfoIn): SimpleSql[Row] = {
      val query =
        """
          |INSERT INTO user_info (user_id, first_name, year_of_birth, sex, weight_kg,
          |                       height_cm, level_of_physical_activity_id)
          |VALUES ({user_id}, {first_name}, {year_of_birth}, {sex}::sex_enum, {weight_kg},
          |        {height_cm}, {level_of_physical_activity_id})
          |ON CONFLICT (user_id) DO UPDATE
          |SET first_name = {first_name},
          |    year_of_birth = {year_of_birth},
          |    sex = {sex}::sex_enum,
          |    weight_kg = {weight_kg},
          |    height_cm = {height_cm},
          |    level_of_physical_activity_id = {level_of_physical_activity_id}
          |RETURNING user_id, first_name, year_of_birth, sex, weight_kg, height_cm, level_of_physical_activity_id;
        """.stripMargin
      SQL(query).on(
        'user_id -> userId,
        'first_name -> userInfo.firstName,
        'year_of_birth -> userInfo.yearOfBirth,
        'sex -> userInfo.sex,
        'weight_kg -> userInfo.weight,
        'height_cm -> userInfo.height,
        'level_of_physical_activity_id -> userInfo.levelOfPhysicalActivityId
      )
    }

    def getSqlGet(userId: Long): SimpleSql[Row] = {
      val query =
        """
          |SELECT user_id, first_name, year_of_birth, sex, weight_kg, height_cm, level_of_physical_activity_id
          |FROM user_info
          |WHERE user_id={user_id};
        """.stripMargin
      SQL(query).on('user_id -> userId)
    }

  }

  def update(userId: Long, userInfo: UserInfoIn): Either[ConstraintError, UserInfoOut] = tryWithConnection {
    implicit conn =>
      tryWithConstraintsCheck[ConstraintError, UserInfoOut](constraintErrorsPartialFn) {
        Right(UserInfoRow.getSqlUpdate(userId, userInfo).executeQuery()
          .as(Macro.namedParser[UserInfoRow].single).toUserInfoOut())
      }
  }

  def get(userId: Long): Either[LookupError, UserInfoOut] = tryWithConnection {
    implicit conn =>
      UserInfoRow.getSqlGet(userId).executeQuery().as(Macro.namedParser[UserInfoRow].singleOpt) match {
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Info for User id: $userId not found")))
        case Some(userInfo) =>
          Right(userInfo.toUserInfoOut())
      }
  }

}
