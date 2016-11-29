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

package uk.ac.ncl.openlab.intake24.sql.tools.system

import anorm._
import org.rogach.scallop._
import java.sql.DriverManager
import scala.xml.XML
import java.io.File
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import java.sql.Connection
import net.scran24.datastore.mongodb.MongoDbDataStore
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.sql.Timestamp
import org.workcraft.gwt.shared.client.Callback1
import net.scran24.datastore.NutritionMappedSurveyRecord
import scala.collection.mutable.Buffer
import java.util.UUID
import uk.ac.ncl.openlab.intake24.datastoresql.Queries
import uk.ac.ncl.openlab.intake24.datastoresql.Util._
import uk.ac.ncl.openlab.intake24.datastoresql.JavaConversions._
import net.scran24.datastore.NutritionMappedSurveyRecordWithId
import java.util.Properties
import uk.ac.ncl.openlab.intake24.datastoresql.JavaConversions
import java.io.BufferedReader
import java.io.InputStreamReader
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConfiguration
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import net.scran24.datastore.SecureUserRecord

class MongoDbImporter(dbConn: Connection, mongoStore: MongoDbDataStore) {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val implicitConn = dbConn

  def importSurveys(ignoreSurveys: Set[String]) = {
    val surveys = mongoStore.getSurveyNames()

    val surveyParams = surveys.filterNot(ignoreSurveys.contains(_)).map(survey_id => (survey_id, mongoStore.getSurveyParameters(survey_id))).map {
      case (survey_id, params) => Seq[NamedParameter]('id -> survey_id, 'state -> params.state.ordinal(), 'start_date -> new Timestamp(params.startDate), 'end_date -> new Timestamp(params.endDate), 'scheme_id -> params.schemeName,
        'locale -> params.locale, 'allow_gen_users -> params.allowGenUsers, 'suspension_reason -> params.suspensionReason, 'survey_monkey_url -> jopt2option(params.surveyMonkeyUrl))
    }

    if (!surveyParams.isEmpty) {
      logger.info("Importing " + surveys.size() + " survey records")
      try {
        BatchSql(Queries.surveysInsert, surveyParams).execute()
      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }
    } else
      logger.info("There are no survey records to import")
  }

  private def importUsersFor(survey_id: String, rename: Option[String] = None) = {
    logger.info("Importing users for " + survey_id)

    val userRecords = mongoStore.getUserRecords(survey_id).asScala.toSeq

    val filteredUserRecords = Buffer[SecureUserRecord]()

    val knownUserNames = scala.collection.mutable.Set[String]()

    userRecords.foreach {

      record =>

        if (knownUserNames.contains(record.username)) {
          logger.warn(s"User name ${record.username} is used more than once, skipping this record!")
        } else {
          filteredUserRecords += record
          knownUserNames += record.username
        }
    }

    if (!filteredUserRecords.isEmpty())
      ???
      //Queries.batchUserInsert(rename.getOrElse(survey_id), filteredUserRecords.map(fromJavaSecureUserRecord))
    else
      logger.info("Survey " + survey_id + " has no user records")
  }

  def importUsers(ignoreSurveys: Set[String]) = {
    importUsersFor("admin", Some(""))
    mongoStore.getSurveyNames().filterNot(ignoreSurveys.contains(_)).foreach(importUsersFor(_))
  }

  private def writeSurveySubmissions(survey_id: String, surveys: Seq[NutritionMappedSurveyRecordWithId]) = {
    val ids = scala.collection.mutable.Map[NutritionMappedSurveyRecordWithId, UUID]()

    val submissionParams = surveys.map {
      record =>
        val id = UUID.randomUUID()
        ids += (record -> id)
        Seq[NamedParameter]('id -> id, 'survey_id -> survey_id, 'user_id -> record.survey.userName, 'start_time -> new Timestamp(record.survey.startTime),
          'end_time -> new Timestamp(record.survey.endTime), 'log -> record.survey.log.mkString("\n"))
    }

    if (!submissionParams.isEmpty) {
      logger.info("Importing " + surveys.size() + " submission records")
      try {
        BatchSql(Queries.surveySubmissionsInsert, submissionParams).execute()
      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }
    } else
      logger.info("There are no submission records to import")

    val customFieldParams = surveys.flatMap {
      record =>
        record.survey.customData.map {
          case (name, value) => Seq[NamedParameter]('survey_submission_id -> ids(record), 'name -> name, 'value -> value)
        }
    }

    if (!customFieldParams.isEmpty) {
      logger.info("Importing " + customFieldParams.size() + " submission custom field records")
      try {
        BatchSql(Queries.surveyCustomFieldsInsert, customFieldParams).execute()
      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }
    } else
      logger.info("There are no custom field records to import")

    val userCustomFieldParams = surveys.flatMap {
      record =>
        record.userCustomFields.map {
          case (name, value) => Seq[NamedParameter]('survey_submission_id -> ids(record), 'name -> name, 'value -> value)
        }
    }

    if (!userCustomFieldParams.isEmpty) {
      logger.info("Importing " + userCustomFieldParams.size() + " submission user custom field records")
      try {
        BatchSql(Queries.surveyUserCustomFieldsInsert, userCustomFieldParams).execute()
      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }
    } else
      logger.info("There are no submission user custom field records to import")

    val submissionMeals = surveys.toSeq.map(s => (ids(s), s.survey.meals))

    val mealParams = submissionMeals.flatMap {
      case (id, meals) =>
        meals.map {
          meal => Seq[NamedParameter]('survey_submission_id -> id, 'hours -> meal.time.hours, 'minutes -> meal.time.minutes, 'name -> meal.name)
        }
    }

    if (!mealParams.isEmpty) {
      logger.info("Importing " + mealParams.size() + " meal records")
      try {
        val batch = BatchSql(Queries.surveyMealsInsert, mealParams)

        val mealIds = batchKeys(batch)

        val meals = mealIds.zip(submissionMeals.flatMap(_._2))

        // Custom fields

        val mealCustomFieldParams = meals.flatMap {
          case (meal_id, meal) =>
            meal.customData.map {
              case (name, value) => Seq[NamedParameter]('meal_id -> meal_id, 'name -> name, 'value -> value)
            }
        }

        if (!mealCustomFieldParams.isEmpty) {
          logger.info("Importing " + mealCustomFieldParams.size() + " meal custom field records")
          BatchSql(Queries.surveyMealsCustomFieldsInsert, mealCustomFieldParams).execute()
        } else
          logger.info("There are no meal custom field records to import")

        // Foods

        val mealFoodsParams = meals.flatMap {
          case (meal_id, meal) =>
            meal.foods.map {
              case food =>

                val truncatedSearchTerm = if (food.searchTerm.length() > 256) {
                  logger.warn(s"""Food search term "${food.searchTerm}" too long, truncating""")
                  food.searchTerm.take(256)
                } else {
                  food.searchTerm
                }

                Seq[NamedParameter]('meal_id -> meal_id, 'code -> food.code, 'english_description -> food.englishDescription, 'local_description -> JavaConversions.jopt2option(food.localDescription), 'ready_meal -> food.isReadyMeal, 'search_term -> truncatedSearchTerm,
                  'portion_size_method_id -> food.portionSize.scriptName, 'reasonable_amount -> food.reasonableAmount, 'food_group_id -> food.foodGroupCode, 'food_group_english_description -> food.foodGroupEnglishDescription,
                  'food_group_local_description -> JavaConversions.jopt2option(food.foodGroupLocalDescription), 'brand -> food.brand, 'nutrient_table_id -> food.nutrientTableID, 'nutrient_table_code -> food.nutrientTableCode)
            }
        }

        if (!mealFoodsParams.isEmpty) {
          logger.info("Importing " + mealFoodsParams.size + " food records")
          val batch = BatchSql(Queries.surveyFoodsInsert, mealFoodsParams)

          val foodIds = batchKeys(batch)

          val foods = foodIds.zip(meals.flatMap(_._2.foods))

          // Food custom fields

          val foodCustomFieldParams = foods.flatMap {
            case (food_id, food) =>
              food.customData.map {
                case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
              }
          }

          if (!foodCustomFieldParams.isEmpty) {
            logger.info("Importing " + foodCustomFieldParams.size() + " food custom field records")
            BatchSql(Queries.surveyFoodCustomFieldsInsert, foodCustomFieldParams).execute()
          } else
            logger.info("There are no food custom field records to import")

          // Food portion size method parameters

          val foodPortionSizeMethodParams = foods.flatMap {
            case (food_id, food) =>
              food.portionSize.data.map {
                case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
              }
          }

          if (!foodPortionSizeMethodParams.isEmpty) {
            logger.info("Importing " + foodPortionSizeMethodParams.size() + " portion size parameter records")
            BatchSql(Queries.surveyFoodPortionSizeFieldsInsert, foodPortionSizeMethodParams).execute()
          } else
            logger.info("There are no portion size parameter records to import")

          // Food nutrient values

          val foodNutrientParams = foods.flatMap {
            case (food_id, food) =>
              food.nutrients.map {
                case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
              }
          }

          if (!foodNutrientParams.isEmpty) {
            logger.info("Importing " + foodNutrientParams.size() + " nutrient records")
            BatchSql(Queries.surveyFoodNutrientValuesInsert, foodNutrientParams).execute()
          } else
            logger.info("There are no nutrient records to import")

        } else
          logger.info("There are no food records to import")

      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }

    } else {
      logger.info("There are no meal records to import")
    }
  }

  private def importSurveySubmissionsFor(survey_id: String) = {
    logger.info("Importing submissions for " + survey_id)

    val userRecords = mongoStore.getUserRecords(survey_id).asScala.map(_.username).toSet

    val surveysBuffer = Buffer[NutritionMappedSurveyRecordWithId]()

    logger.info("Retrieving submission records from MongoDB")

    var counter = 0

    mongoStore.processSurveys(survey_id, Long.MinValue, Long.MaxValue, new Callback1[NutritionMappedSurveyRecordWithId] {
      def call(record: NutritionMappedSurveyRecordWithId) = {

        if (!userRecords.contains(record.survey.userName)) {
          logger.warn(s"User record missing for ${record.survey.userName} (survey submission ${record.id}), skipping!")
        } else {

          if (surveysBuffer.size == 500) {
            writeSurveySubmissions(survey_id, surveysBuffer)
            counter += surveysBuffer.size
            logger.info(s"... $counter surveys processed")
            surveysBuffer.clear()
          }

          surveysBuffer += record
        }
      }
    })

    writeSurveySubmissions(survey_id, surveysBuffer)
    counter += surveysBuffer.size
    logger.info(s"... $counter surveys processed")

  }

  def importSurveySubmissions(ignoreSurveys: Set[String]) = {
    val surveys = mongoStore.getSurveyNames()

    surveys.filterNot(ignoreSurveys.contains(_)).foreach(importSurveySubmissionsFor)
  }

  def importMongoDbData(ignoreSurveys: Set[String] = Set()) = {
    importSurveys(ignoreSurveys)
    importUsers(ignoreSurveys)
    importSurveySubmissions(ignoreSurveys)
  }
}

trait ImportOptions extends ScallopConf {
  version("Intake24 MongoDB to SQL migration tool 2.0.0-SNAPSHOT")

  val mongoHost = opt[String](required = true, noshort = true)
  val mongoDatabase = opt[String](required = true, noshort = true)
  val mongoUser = opt[String](noshort = true)
  val mongoPassword = opt[String](noshort = true)

  val noWarning = opt[Boolean](noshort = true)

  def mongoConfiguration = MongoConfiguration(mongoHost(), mongoDatabase(), mongoUser.get, mongoPassword.get)
}

case class MongoConfiguration(host: String, database: String, user: Option[String], password: Option[String])

object MongoDbImportConsole extends App with WarningMessage {

  val options = new ScallopConf(args) with ImportOptions with DatabaseOptions

  options.verify()

  if (!options.noWarning())
    displayWarningMessage("Please make sure that the database has been initialised with the Init tool first.")

  MongoDbImport.run(options.databaseConfig, options.mongoConfiguration, Set())

}

object MongoDbImport extends DatabaseConnection {

  val logger = LoggerFactory.getLogger(getClass)

  def run(databaseConfig: DatabaseConfiguration, mongoConfig: MongoConfiguration, ignoreSurveys: Set[String]) = {

    val ds = getDataSource(databaseConfig)

    val mongoDataStore = new MongoDbDataStore(mongoConfig.host, 27017, mongoConfig.database, mongoConfig.user.getOrElse(""), mongoConfig.password.getOrElse(""))

    val connection = ds.getConnection

    val importer = new MongoDbImporter(connection, mongoDataStore)

    importer.importMongoDbData(ignoreSurveys)

    connection.close()
  }
}