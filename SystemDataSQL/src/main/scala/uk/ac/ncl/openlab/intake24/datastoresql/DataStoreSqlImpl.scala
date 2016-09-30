/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.datastoresql

import java.sql.Connection
import anorm._
import java.sql.Timestamp
import net.scran24.datastore.DataStoreException
import java.time.LocalDateTime
import scala.annotation.tailrec
import java.util.UUID
import org.slf4j.LoggerFactory
import scala.util.Try

import scala.util.Failure
import com.google.inject.Singleton
import com.google.inject.Inject
import com.google.inject.name.Named
import javax.sql.DataSource
import scala.util.Random
import org.postgresql.util.PSQLException

@Singleton
class DataStoreSqlImpl @Inject() (@Named("intake24_system") dataSource: DataSource) extends DataStoreScala {

  val logger = LoggerFactory.getLogger(classOf[DataStoreSqlImpl])

  def tryWithConnection[T](block: Connection => T) = {
    val conn = dataSource.getConnection()
    try {
      block(conn)
    } catch {
      case e: java.sql.BatchUpdateException => throw new DataStoreException(e.getNextException)
      case e: Throwable => throw new DataStoreException(e)
    } finally {
      conn.close()
    }
  }

  def initSurvey(survey_id: String, scheme_name: String, locale: String, allowGenUsers: Boolean, surveyMonkeyUrl: Option[String]) = tryWithConnection {
    implicit conn =>
      SQL(Queries.surveysInsertInit)
        .on('id -> survey_id, 'scheme_id -> scheme_name, 'locale -> locale, 'allow_gen_users -> allowGenUsers, 'survey_monkey_url -> surveyMonkeyUrl)
        .execute()
  }

  def getSurveyNames() = tryWithConnection {
    implicit conn =>
      SQL("SELECT id FROM surveys WHERE id <> ''").executeQuery().as(SqlParser.str("id").*)
  }

  def deleteUsers(survey_id: String, role: String) = tryWithConnection {
    implicit conn => SQL(Queries.usersDeleteByRole).on('role -> role, 'survey_id -> survey_id).execute()
  }

  private case class UserDataRow(name: String, value: String)

  def getUserData(survey_id: String, user_id: String): Map[String, String] = tryWithConnection {
    implicit conn =>
      val parser = Macro.namedParser[UserDataRow]
      val rows = SQL(Queries.userCustomFieldsSelectByUser)
        .on('survey_id -> survey_id, 'user_id -> user_id)
        .executeQuery().as(parser.*).map(row => (row.name, row.value)).toMap
      rows
  }

  def setUserData(survey_id: String, user_id: String, userData: Map[String, String]) = tryWithConnection {
    implicit conn =>

      conn.setAutoCommit(false)
      SQL(Queries.userCustomFieldsDelete).on('survey_id -> survey_id, 'user_id -> user_id).executeQuery()

      val insertParams = userData.map {
        case (name, value) => Seq[NamedParameter]('survey_id -> survey_id, 'user_id -> user_id, 'name -> name, 'value -> value)
      }.toSeq

      if (!insertParams.isEmpty)

        try {
          BatchSql(Queries.userCustomFieldsInsert, insertParams).execute()
        } catch {
          case e: java.sql.BatchUpdateException => throw e.getNextException
        }

      conn.commit()
  }

  def saveUsers(survey_id: String, users: Seq[SecureUserRecord]): Unit = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)
      Queries.batchUserInsert(survey_id, users)
      conn.commit()
  }

  def addUser(survey_id: String, userRecord: SecureUserRecord): Unit = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)
      SQL(Queries.usersInsert)
        .on(
          'id -> userRecord.username,
          'survey_id -> survey_id,
          'password_hash -> userRecord.passwordHashBase64,
          'password_salt -> userRecord.passwordSaltBase64,
          'password_hasher -> userRecord.passwordHasher)
        .execute()

      try {
        val roleParams = userRecord.roles.map(role => Seq[NamedParameter]('survey_id -> survey_id, 'user_id -> userRecord.username, 'role -> role)).toSeq

        if (!roleParams.isEmpty)
          BatchSql(Queries.userRolesInsert, roleParams).execute()

        val permissionParams = userRecord.permissions.map(permission => Seq[NamedParameter]('survey_id -> survey_id, 'user_id -> userRecord.username, 'permission -> permission)).toSeq

        if (!permissionParams.isEmpty)
          BatchSql(Queries.userPermissionsInsert, permissionParams).execute()

        val userCustomFieldParams = userRecord.customFields.toSeq.map {
          case (name, value) => Seq[NamedParameter]('survey_id -> survey_id, 'user_id -> userRecord.username, 'name -> name, 'value -> value)
        }

        if (!userCustomFieldParams.isEmpty)
          BatchSql(Queries.userCustomFieldsInsert, userCustomFieldParams).execute()
      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }

      conn.commit()
  }

  private case class ShortUserRecordRow(password_hash: String, password_salt: String, password_hasher: String)

  def getUserRecord(survey_id: String, username: String): Option[SecureUserRecord] = tryWithConnection {
    implicit conn =>
      SQL(Queries.userSelect)
        .on('survey_id -> survey_id, 'user_id -> username).executeQuery()
        .as(Macro.namedParser[ShortUserRecordRow].singleOpt)
        .map {
          row =>
            val roles = SQL(Queries.userRolesSelectByUser).on('survey_id -> survey_id, 'user_id -> username).as(SqlParser.str("role").*)
            val permissions = SQL(Queries.userPermissionsSelectByUser).on('survey_id -> survey_id, 'user_id -> username).as(SqlParser.str("permission").*)
            val custom_fields = SQL(Queries.userCustomFieldsSelectByUser).on('survey_id -> survey_id, 'user_id -> username).as((SqlParser.str("name") ~ SqlParser.str("permission")).*).map {
              case name ~ value => (name, value)
            }.toMap

            SecureUserRecord(username, row.password_hash, row.password_salt, row.password_hasher, roles.toSet, permissions.toSet, custom_fields)
        }
  }

  private case class UserRecordRow(survey_id: String, user_id: String, password_hash: String, password_salt: String, password_hasher: String)

  private case class RoleRecordRow(survey_id: String, user_id: String, role: String)

  private case class PermissionRecordRow(survey_id: String, user_id: String, permission: String)

  private case class CustomFieldRecordRow(survey_id: String, user_id: String, name: String, value: String)

  @tailrec
  private def buildUserRecords(
    userRows: List[UserRecordRow],
    roleRows: List[RoleRecordRow],
    permRows: List[PermissionRecordRow],
    customFieldRows: List[CustomFieldRecordRow],
    result: List[SecureUserRecord] = List()): List[SecureUserRecord] = userRows match {
    case Nil => result.reverse
    case curUserRow :: restOfUserRows =>
      {
        val (curUserRoleRows, restOfRoleRows) = roleRows.span(r => (r.survey_id == curUserRow.survey_id && r.user_id == curUserRow.user_id))
        val (curUserPermRows, restOfPermRows) = permRows.span(r => (r.survey_id == curUserRow.survey_id && r.user_id == curUserRow.user_id))
        val (curUserFieldRows, restOfFieldRows) = customFieldRows.span(r => (r.survey_id == curUserRow.survey_id && r.user_id == curUserRow.user_id))

        val curUserRoles = curUserRoleRows.map(_.role)
        val curUserPerms = curUserPermRows.map(_.permission)
        val curUserFields = curUserFieldRows.map(f => (f.name, f.value)).toMap

        val record = SecureUserRecord(curUserRow.user_id, curUserRow.password_hash, curUserRow.password_salt,
          curUserRow.password_hasher, curUserRoles.toSet, curUserPerms.toSet, curUserFields)

        buildUserRecords(restOfUserRows, restOfRoleRows, restOfPermRows, restOfFieldRows, record +: result)
      }
  }

  def getUserRecords(survey_id: String, role: String): Seq[SecureUserRecord] = tryWithConnection {
    implicit conn =>
      val userRows = SQL(Queries.usersSelectByRole).on('survey_id -> survey_id, 'role -> role).executeQuery().as(Macro.namedParser[UserRecordRow].*)

      val roleRows = SQL(Queries.userRolesSelectByRole).on('survey_id -> survey_id, 'role -> role).executeQuery().as(Macro.namedParser[RoleRecordRow].*)

      val permRows = SQL(Queries.userPermissionsSelectByRole).on('survey_id -> survey_id, 'role -> role).executeQuery().as(Macro.namedParser[PermissionRecordRow].*)

      val customFieldRows = SQL(Queries.userCustomFieldsSelectByRole).on('survey_id -> survey_id, 'role -> role).executeQuery().as(Macro.namedParser[CustomFieldRecordRow].*)

      buildUserRecords(userRows, roleRows, permRows, customFieldRows)
  }

  def getUserRecords(survey_id: String): Seq[SecureUserRecord] = tryWithConnection {
    implicit conn =>
      val userRows = SQL(Queries.usersSelectBySurvey).on('survey_id -> survey_id).executeQuery().as(Macro.namedParser[UserRecordRow].*)

      val roleRows = SQL(Queries.userRolesSelectBySurvey).on('survey_id -> survey_id).executeQuery().as(Macro.namedParser[RoleRecordRow].*)

      val permRows = SQL(Queries.userPermissionsSelectBySurvey).on('survey_id -> survey_id).executeQuery().as(Macro.namedParser[PermissionRecordRow].*)

      val customFieldRows = SQL(Queries.userCustomFieldsSelectBySurvey).on('survey_id -> survey_id).executeQuery().as(Macro.namedParser[CustomFieldRecordRow].*)

      buildUserRecords(userRows, roleRows, permRows, customFieldRows)
  }

  def saveSurvey(survey_id: String, username: String, survey: NutritionMappedSurveyRecord): Unit = tryWithConnection {
    implicit conn =>

      conn.setAutoCommit(false)

      val generated_id = java.util.UUID.randomUUID()

      SQL(Queries.surveySubmissionsInsert)
        .on('id -> generated_id, 'survey_id -> survey_id, 'user_id -> survey.survey.userName, 'start_time -> new Timestamp(survey.survey.startTime),
          'end_time -> new Timestamp(survey.survey.endTime), 'log -> survey.survey.log.mkString("\n"))
        .execute()

      val customFieldParams = survey.survey.customData.map {
        case (name, value) => Seq[NamedParameter]('survey_submission_id -> generated_id, 'name -> name, 'value -> value)
      }.toSeq

      if (!customFieldParams.isEmpty) {
        BatchSql(Queries.surveyCustomFieldsInsert, customFieldParams).execute()
      }

      val userCustomFieldParams = survey.userCustomFields.map {
        case (name, value) => Seq[NamedParameter]('survey_submission_id -> generated_id, 'name -> name, 'value -> value)
      }.toSeq

      if (!userCustomFieldParams.isEmpty) {
        BatchSql(Queries.surveyUserCustomFieldsInsert, userCustomFieldParams).execute()
      }

      // Meals

      if (!survey.survey.meals.isEmpty) {

        val mealParams = survey.survey.meals.map {
          meal =>
            Seq[NamedParameter]('survey_submission_id -> generated_id, 'hours -> meal.time.hours, 'minutes -> meal.time.minutes, 'name -> meal.name)
        }

        val batch = BatchSql(Queries.surveyMealsInsert, mealParams)

        val mealIds = Util.batchKeys(batch)

        val meals = mealIds.zip(survey.survey.meals)

        // Custom fields

        val mealCustomFieldParams = meals.flatMap {
          case (meal_id, meal) =>
            meal.customData.map {
              case (name, value) => Seq[NamedParameter]('meal_id -> meal_id, 'name -> name, 'value -> value)
            }
        }

        if (!mealCustomFieldParams.isEmpty) {
          BatchSql(Queries.surveyMealsCustomFieldsInsert, mealCustomFieldParams).execute()
        }

        // Foods

        val mealFoodsParams = meals.flatMap {
          case (meal_id, meal) =>
            meal.foods.map {
              case food =>
                Seq[NamedParameter]('meal_id -> meal_id, 'code -> food.code, 'english_description -> food.englishDescription, 'local_description -> food.localDescription, 'ready_meal -> food.isReadyMeal, 'search_term -> food.searchTerm,
                  'portion_size_method_id -> food.portionSize.scriptName, 'reasonable_amount -> food.reasonableAmount, 'food_group_id -> food.foodGroupCode, 'food_group_english_description -> food.foodGroupEnglishDescription,
                  'food_group_local_description -> food.foodGroupLocalDescription, 'brand -> food.brand, 'nutrient_table_id -> food.nutrientTableID, 'nutrient_table_code -> food.nutrientTableCode)
            }
        }

        if (!mealFoodsParams.isEmpty) {

          val batch = BatchSql(Queries.surveyFoodsInsert, mealFoodsParams)

          val foodIds = Util.batchKeys(batch)

          val foods = foodIds.zip(meals.flatMap(_._2.foods))

          // Food custom fields

          val foodCustomFieldParams = foods.flatMap {
            case (food_id, food) =>
              food.customData.map {
                case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
              }
          }

          if (!foodCustomFieldParams.isEmpty) {
            BatchSql(Queries.surveyFoodCustomFieldsInsert, foodCustomFieldParams).execute()
          }

          // Food portion size method parameters

          val foodPortionSizeMethodParams = foods.flatMap {
            case (food_id, food) =>
              food.portionSize.data.map {
                case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
              }
          }

          if (!foodPortionSizeMethodParams.isEmpty) {
            BatchSql(Queries.surveyFoodPortionSizeFieldsInsert, foodPortionSizeMethodParams).execute()
          }

          // Food nutrient values

          val foodNutrientParams = foods.flatMap {
            case (food_id, food) =>
              food.nutrients.map {
                case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
              }
          }

          if (!foodNutrientParams.isEmpty) {
            BatchSql(Queries.surveyFoodNutrientValuesInsert, foodNutrientParams).execute()
          }
        }
      }

      conn.commit()
  }

  def saveMissingFoods(missingFoods: Seq[MissingFoodRecord]): Unit = tryWithConnection {
    implicit conn =>
      if (!missingFoods.isEmpty) {
        val params = missingFoods.map {
          record =>
            Seq[NamedParameter]('submitted_at -> new Timestamp(record.submittedAt), 'survey_id -> record.surveyId, 'user_id -> record.userName,
              'name -> record.name, 'brand -> record.brand, 'description -> record.description, 'portion_size -> record.portionSize, 'leftovers -> record.leftovers)
        }

        BatchSql(Queries.missingFoodsInsert, params).execute()
      }
  }

  case class MissingFoodRow(survey_id: String, user_id: String, name: String, brand: String, description: String, portion_size: String, leftovers: String, submitted_at: LocalDateTime)

  @tailrec
  private def processNextMissingFoodRecord(cursor: Option[Cursor], callback: MissingFoodRecord => Unit)(implicit connection: Connection): Unit = cursor match {
    case Some(cursor) => cursor.row.as(Macro.namedParser[MissingFoodRow]) match {
      case scala.util.Success(row) => {
        callback(MissingFoodRecord(Timestamp.valueOf(row.submitted_at).getTime(), row.survey_id, row.user_id, row.name,
          row.brand, row.description, row.portion_size, row.leftovers))
        processNextMissingFoodRecord(cursor.next, callback)
      }
      case Failure(e) => throw new DataStoreException(e)
    }
    case None => ()
  }

  def processMissingFoods(timeFrom: Long, timeTo: Long, processMissingFood: MissingFoodRecord => Unit): Unit = tryWithConnection {
    implicit conn =>
      SQL(Queries.missingFoodsSelect)
        .on('time_from -> new Timestamp(timeFrom), 'time_to -> new Timestamp(timeTo))
        .withResult(cursor => processNextMissingFoodRecord(cursor, processMissingFood))
  }

  case class SubmissionRecordRow(id: UUID, survey_id: String, user_id: String, start_time: LocalDateTime, end_time: LocalDateTime, log: String)

  case class SubmissionMealRow(meal_id: Long, hours: Int, minutes: Int, name: String, cf_name: Option[String], cf_value: Option[String])

  case class SubmissionFoodRow(meal_id: Long, food_id: Long, code: String, english_description: String, local_description: Option[String], ready_meal: Boolean, search_term: String,
    portion_size_method_id: String, reasonable_amount: Boolean, food_group_id: Long, food_group_english_description: String, food_group_local_description: Option[String],
    brand: String, nutrient_table_id: String, nutrient_table_code: String, cf_name: Option[String], cf_value: Option[String])

  case class SubmissionNutrientRow(food_id: Long, n_name: String, n_amount: Double)

  case class SubmissionPortionSizeDataRow(food_id: Long, name: String, value: String)

  @tailrec
  private def processNextRecord(cursor: Option[Cursor], callback: NutritionMappedSurveyRecordWithId => Unit)(implicit connection: Connection): Unit = cursor match {
    case Some(cursor) => cursor.row.as(Macro.namedParser[SubmissionRecordRow]) match {
      case scala.util.Success(row) => {

        val customFields =
          SQL(Queries.surveySubmissionCustomFieldsSelect)
            .on('survey_submission_id -> row.id)
            .as((SqlParser.str("name") ~ SqlParser.str("value")).*)
            .map {
              case name ~ value => name -> value
            }.toMap

        val userCustomFields =
          SQL(Queries.surveySubmissionUserCustomFieldsSelect)
            .on('survey_submission_id -> row.id)
            .as((SqlParser.str("name") ~ SqlParser.str("value")).*)
            .map {
              case name ~ value => name -> value
            }.toMap

        val mealRows = SQL(Queries.surveySubmissionMealsSelect)
          .on('survey_submission_id -> row.id)
          .as(Macro.namedParser[SubmissionMealRow].*)
          .groupBy(_.meal_id)

        val foodRows = SQL(Queries.surveySubmissionFoodsSelect)
          .on('survey_submission_id -> row.id)
          .as(Macro.namedParser[SubmissionFoodRow].*)
          .groupBy(_.meal_id)
          .mapValues(_.groupBy(_.food_id))

        val nutrientRows = SQL(Queries.surveySubmissionNutrientsSelect)
          .on('survey_submission_id -> row.id)
          .as(Macro.namedParser[SubmissionNutrientRow].*)
          .groupBy(_.food_id)

        val portionSizeData = SQL(Queries.surveySubmissionPortionSizeDataSelect)
          .on('survey_submission_id -> row.id)
          .as(Macro.namedParser[SubmissionPortionSizeDataRow].*)
          .groupBy(_.food_id)

        val meals = mealRows.keys.toSeq.sorted.map {
          meal_id =>
            val rows = mealRows(meal_id)

            val foods = foodRows.get(meal_id) match {
              // match block prevents NoSuchElementException if the meal has no foods             
              case Some(foods) => foods.keys.toSeq.sorted.map {
                food_id =>
                  val rows = foods(food_id)

                  val customFields = rows.filter(r => r.cf_name.nonEmpty && r.cf_value.nonEmpty).map(r => (r.cf_name.get -> r.cf_value.get)).toMap

                  val nutrients = nutrientRows.get(food_id) match {
                    case Some(rows) => rows.map(r => (r.n_name -> r.n_amount)).toMap
                    case None => Map[String, Double]()
                  }

                  val portionSizeFields = portionSizeData.get(food_id) match {
                    case Some(fields) => fields.map(r => (r.name -> r.value)).toMap
                    case None => Map[String, String]()
                  }

                  val portionSize = CompletedPortionSize(rows.head.portion_size_method_id, portionSizeFields)

                  NutritionMappedFood(rows.head.code, rows.head.english_description, rows.head.local_description, rows.head.nutrient_table_id, rows.head.nutrient_table_code, rows.head.ready_meal, rows.head.search_term, portionSize,
                    rows.head.food_group_id.toInt, rows.head.food_group_english_description, rows.head.food_group_local_description, rows.head.reasonable_amount, rows.head.brand, nutrients, customFields)
              }.toList
              case None => Seq()
            }

            val customFields = rows.filter(r => r.cf_name.nonEmpty && r.cf_value.nonEmpty).map(r => (r.cf_name.get -> r.cf_value.get)).toMap

            NutritionMappedMeal(
              rows.head.name,
              foods,
              MealTime(rows.head.hours, rows.head.minutes),
              customFields)
        }.toList

        val survey = NutritionMappedSurvey(Timestamp.valueOf(row.start_time).getTime, Timestamp.valueOf(row.end_time).getTime, meals, row.log.split("\n").toList.filterNot(_.isEmpty()), row.user_id, customFields)

        callback(NutritionMappedSurveyRecordWithId(row.id.toString(), survey, userCustomFields))

        processNextRecord(cursor.next, callback)
      }

      case Failure(f) => throw new DataStoreException(f)
    }
    case None => ()
  }

  def processSurveys(survey_id: String, timeFrom: Long, timeTo: Long, processSurvey: NutritionMappedSurveyRecordWithId => Unit): Unit = tryWithConnection {
    implicit conn =>
      SQL(Queries.surveySubmissionsSelectByTime)
        .on('survey_id -> survey_id, 'time_from -> new Timestamp(timeFrom), 'time_to -> new Timestamp(timeTo))
        .withResult(cursor => processNextRecord(cursor, processSurvey)) match {
          case Left(errors) => throw new DataStoreException(errors.head)
          case Right(_) => ()
        }
  }

  case class SurveyParameterRow(state: Int, start_date: LocalDateTime, end_date: LocalDateTime, scheme_id: String, locale: String, allow_gen_users: Boolean, suspension_reason: Option[String], survey_monkey_url: Option[String])

  def getSurveyParameters(survey_id: String): SurveyParameters = tryWithConnection {
    implicit conn =>
      val row = SQL(Queries.surveysSelect).on('survey_id -> survey_id).executeQuery().as(Macro.namedParser[SurveyParameterRow].single)

      SurveyParameters(row.state, Timestamp.valueOf(row.start_date).getTime, Timestamp.valueOf(row.end_date).getTime,
        row.scheme_id, row.locale, row.allow_gen_users, row.suspension_reason.getOrElse(""), row.survey_monkey_url)
  }

  def setSurveyParameters(survey_id: String, newParameters: SurveyParameters): Unit = tryWithConnection {
    implicit conn =>
      SQL(Queries.surveysUpdate)
        .on('state -> newParameters.state,
          'start_date -> new Timestamp(newParameters.startDate),
          'end_date -> new Timestamp(newParameters.endDate),
          'scheme_id -> newParameters.schemeName,
          'locale -> newParameters.locale,
          'allow_gen_users -> newParameters.allowGenUsers,
          'suspension_reason -> newParameters.suspensionReason,
          'survey_monkey_url -> newParameters.surveyMonkeyUrl,
          'survey_id -> survey_id)
        .executeUpdate() match {
          case 1 => ()
          case _ => throw new DataStoreException("Survey update rows affected != 1 -- incorrect survey id?")
        }
  }

  def getPopularityCount(foodCodes: Seq[String]): Map[String, Int] = tryWithConnection {
    implicit conn =>
      if (foodCodes.isEmpty)
        Map()
      else {
        val counters = SQL(Queries.popularityCountersSelect)
          .on('food_codes -> foodCodes)
          .executeQuery()
          .as((SqlParser.str("food_code") ~ SqlParser.int("counter")).*)
          .map {
            case food_code ~ counter => food_code -> counter
          }
          .toMap

        foodCodes.map(code => (code, counters.getOrElse(code, 0))).toMap
      }
  }

  def incrementPopularityCount(foodCodes: Seq[String]): Unit = tryWithConnection {
    implicit conn =>
      // Postgres currently does not have native support for UPSERT (as of version 9.4)
      // This code implements an UPDATE / INSERT loop which is the only correct way to handle
      // concurrent writes as of this time.
      // See https://wiki.postgresql.org/wiki/UPSERT
      @tailrec
      def retry(times: Int, items: Seq[String]): Unit = {
        if (items.isEmpty) ()
        else if (times == 0) throw new DataStoreException("Could not upsert popularity counters in a reasonable number of attempts")
        else {

          val updateParams = items.map(code => Seq[NamedParameter]('food_code -> code))

          val tryInsertItems = {
            val updateResult =
              BatchSql(Queries.popularityCounterIncrement, updateParams).execute()

            // Successfull updates will return 1 as number of rows affected
            // everything else indicates failure
            items.zip(updateResult).filterNot(_._2 == 1).map(_._1)
          }

          // Bad performance: 
          // Postgres will throw PSQLException on errors and stop batch execution in case of errors,
          // so each individual item has to be processed using a single query 

          val retryItems = {
            val insertResult = tryInsertItems.map(item => Try {
              SQL(Queries.popularityCounterInsert)
                .on('food_code -> item)
                .executeInsert(SqlParser.str("food_code").single)
            })

            tryInsertItems.zip(insertResult).filter(_._2.isFailure).map(_._1)
          }

          retry(times - 1, retryItems)
        }
      }

      if (foodCodes.isEmpty)
        ()
      else
        retry(10, foodCodes)
  }

  def setGlobalValue(name: String, value: String): Unit = tryWithConnection {
    implicit conn =>
      @tailrec
      def retry(times: Int): Unit = {
        val updateResult = SQL(Queries.globalValueUpdate)
          .on('value -> value, 'name -> name)
          .executeUpdate()

        if (updateResult != 1) {
          try {
            SQL(Queries.globalValueInsert)
              .on('value -> value, 'name -> name)
              .executeInsert(SqlParser.str("name").+)
          } catch {
            case e: Throwable => if (times == 0) throw new DataStoreException("Could not upsert a global value in a reasonable number of attempts") else retry(times - 1)
          }
        }
      }

      retry(10)
  }

  def getGlobalValue(name: String): Option[String] = tryWithConnection {
    implicit conn =>
      SQL(Queries.globalValueSelect)
        .on('name -> name)
        .executeQuery()
        .as(SqlParser.str("value").singleOpt)
  }

  private case class SupportStaffRow(name: String, phone: Option[String], email: Option[String])

  def getSupportStaffRecords(): Seq[SupportStaffRecord] = tryWithConnection {
    implicit conn =>
      SQL(Queries.supportStaffSelectAll)
        .executeQuery()
        .as(Macro.namedParser[SupportStaffRow].*)
        .map {
          row =>
            SupportStaffRecord(row.name, row.phone, row.email)
        }
  }

  case class LastHelpRequestTimeRow(last_help_request_time: LocalDateTime)

  def getLastHelpRequestTime(survey: String, username: String): Option[Long] = tryWithConnection {
    implicit conn =>
      SQL(Queries.lastHelpRequestTimeSelect)
        .on('survey_id -> survey, 'user_id -> username)
        .executeQuery()
        .as(Macro.namedParser[LastHelpRequestTimeRow].singleOpt)
        .map(t => Timestamp.valueOf(t.last_help_request_time).getTime())
  }

  def setLastHelpRequestTime(survey: String, username: String, time: Long): Unit = tryWithConnection {
    implicit conn =>

      @tailrec
      def retry(times: Int): Unit = {
        val updateResult = SQL(Queries.lastHelpRequestTimeUpdate)
          .on('survey_id -> survey, 'user_id -> username, 'time -> new Timestamp(time))
          .executeUpdate()

        if (updateResult != 1) {
          try {
            SQL(Queries.lastHelpRequestTimeInsert)
              .on('survey_id -> survey, 'user_id -> username, 'time -> new Timestamp(time))
              .executeInsert(SqlParser.str("survey_id").single)
          } catch {
            case e: Throwable => if (times == 0) throw new DataStoreException("Could not upsert help request time in a reasonable number of attempts") else retry(times - 1)
          }
        }
      }

      retry(10)
  }

  def generateCompletionCode(survey: String, username: String, external_user_id: String) = tryWithConnection {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    implicit conn =>
      val code = Seq.fill(6)(alphabet.charAt(Random.nextInt(alphabet.length()))).mkString

      SQL("""INSERT INTO external_test_users VALUES (DEFAULT, {survey_id}, {username}, {external_user_id}, {code})""")
        .on('survey_id -> survey, 'username -> username, 'external_user_id -> external_user_id, 'code -> code)
        .executeInsert()

      code
  }

  def validateCompletionCode(survey: String, external_user_id: String, code: String) = tryWithConnection {
    implicit conn =>
      SQL("""SELECT id FROM external_test_users WHERE survey_id={survey_id} AND external_user_id={external_user_id} AND code={code}""")
        .on('survey_id -> survey, 'external_user_id -> external_user_id, 'code -> code)
        .executeQuery()
        .as(SqlParser.long("id").*)
        .nonEmpty
  }
}