package uk.ac.ncl.openlab.intake24.systemsql.user

import java.time.LocalDate
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{UserPhysicalDataIn, UserPhysicalDataOut, UserPhysicalDataService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */
class UserPhysicalDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends UserPhysicalDataService
  with SqlDataService with SqlResourceLoader {

  val constraintErrorsPartialFn = PartialFunction[String, PSQLException => ConstraintError] {
    constraintName => (e: PSQLException) => ConstraintViolation(e.toString, e)
  }

  private case class UserInfoRow(user_id: Long, first_name: Option[String], sex: Option[String],
                                 birthdate: Option[LocalDate], weight_kg: Option[Double], height_cm: Option[Double],
                                 level_of_physical_activity_id: Option[Long]) {
    def toUserInfoOut(): UserPhysicalDataOut = {
      UserPhysicalDataOut(this.user_id, this.first_name, this.sex, this.birthdate, this.weight_kg,
        this.height_cm, this.level_of_physical_activity_id)
    }
  }

  private object UserInfoRow {
    def getSqlUpdate(userId: Long, userInfo: UserPhysicalDataIn): SimpleSql[Row] = {
      val query =
        """
          |INSERT INTO user_physical_data (user_id, first_name, birthdate, sex, weight_kg,
          |                       height_cm, level_of_physical_activity_id)
          |VALUES ({user_id}, {first_name}, {birthdate}, {sex}::sex_enum, {weight_kg},
          |        {height_cm}, {level_of_physical_activity_id})
          |ON CONFLICT (user_id) DO UPDATE
          |SET first_name = {first_name},
          |    birthdate = {birthdate},
          |    sex = {sex}::sex_enum,
          |    weight_kg = {weight_kg},
          |    height_cm = {height_cm},
          |    level_of_physical_activity_id = {level_of_physical_activity_id}
          |RETURNING user_id, first_name, birthdate, sex, weight_kg, height_cm, level_of_physical_activity_id;
        """.stripMargin
      SQL(query).on(
        'user_id -> userId,
        'birthdate -> userInfo.birthdate.map(_.atStartOfDay()), // anorm doesn't know how to handle LocalDate
        'sex -> userInfo.sex,
        'weight_kg -> userInfo.weight,
        'height_cm -> userInfo.height,
        'level_of_physical_activity_id -> userInfo.levelOfPhysicalActivityId
      )
    }

    def getSqlGet(userId: Long): SimpleSql[Row] = {
      val query =
        """
          |SELECT user_id, first_name, birthdate, sex, weight_kg, height_cm, level_of_physical_activity_id
          |FROM user_physical_data
          |WHERE user_id={user_id};
        """.stripMargin
      SQL(query).on('user_id -> userId)
    }

  }

  def update(userId: Long, userInfo: UserPhysicalDataIn): Either[ConstraintError, UserPhysicalDataOut] = tryWithConnection {
    implicit conn =>
      tryWithConstraintsCheck[ConstraintError, UserPhysicalDataOut](constraintErrorsPartialFn) {
        Right(UserInfoRow.getSqlUpdate(userId, userInfo).executeQuery()
          .as(Macro.namedParser[UserInfoRow].single).toUserInfoOut())
      }
  }

  def get(userId: Long): Either[LookupError, UserPhysicalDataOut] = tryWithConnection {
    implicit conn =>
      UserInfoRow.getSqlGet(userId).executeQuery().as(Macro.namedParser[UserInfoRow].singleOpt) match {
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Info for User id: $userId not found")))
        case Some(userInfo) =>
          Right(userInfo.toUserInfoOut())
      }
  }

  def batchUpdateQuery(update: Map[Long, UserPhysicalDataIn])(implicit connection: java.sql.Connection): Either[ConstraintError, Unit] =
    if (update.isEmpty)
      Right(())
    else {
      val params = update.toSeq.map {
        case (userId, userInfo) =>
          Seq[NamedParameter]('user_id -> userId, 'first_name -> userInfo.firstName,
            'birthdate -> userInfo.birthdate.map(_.atStartOfDay()), 'sex -> userInfo.sex, // anorm doesn't know how to handle LocalDate
            'weight_kg -> userInfo.weight, 'height_cm -> userInfo.height,
            'level_of_physical_activity_id -> userInfo.levelOfPhysicalActivityId)
      }

      val query =
        """|INSERT INTO user_physical_data (user_id, first_name, birthdate, sex, weight_kg,
           |                       height_cm, level_of_physical_activity_id)
           |VALUES ({user_id}, {first_name}, {birthdate}, {sex}::sex_enum, {weight_kg},
           |        {height_cm}, {level_of_physical_activity_id})
           |ON CONFLICT (user_id) DO UPDATE
           |SET first_name = {first_name},
           |    birthdate = {birthdate},
           |    sex = {sex}::sex_enum,
           |    weight_kg = {weight_kg},
           |    height_cm = {height_cm},
           |    level_of_physical_activity_id = {level_of_physical_activity_id}""".stripMargin

      tryWithConstraintsCheck[ConstraintError, Unit](constraintErrorsPartialFn) {
        BatchSql(query, params.head, params.tail: _*).execute()
        Right(())
      }
    }
}
