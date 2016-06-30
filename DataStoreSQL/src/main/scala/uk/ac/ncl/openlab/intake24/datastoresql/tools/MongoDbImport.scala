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

package uk.ac.ncl.openlab.intake24.datastoresql.tools

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

class MongoDbImporter(dbConn: Connection, mongoStore: MongoDbDataStore) {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val implicitConn = dbConn

  def importSurveys() = {
    val surveys = mongoStore.getSurveyNames()

    val surveyParams = surveys.map(survey_id => (survey_id, mongoStore.getSurveyParameters(survey_id))).map {
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
      logger.warn("There are no survey records to import")
  }

  private def importUsersFor(survey_id: String, rename: Option[String] = None) = {
    logger.info("Importing users for " + survey_id)

    val userRecords = mongoStore.getUserRecords(survey_id)

    if (!userRecords.isEmpty())
      Queries.batchUserInsert(rename.getOrElse(survey_id), userRecords.map(fromJavaSecureUserRecord))
    else
      logger.warn("Survey " + survey_id + " has no user records")
  }

  def importUsers() = {
    importUsersFor("admin", Some(""))
    mongoStore.getSurveyNames().foreach(importUsersFor(_))
  }

  private def importSurveySubmissionsFor(survey_id: String) = {
    logger.info("Importing submissions for " + survey_id)

    val surveysBuffer = Buffer[NutritionMappedSurveyRecordWithId]()

    val ids = scala.collection.mutable.Map[NutritionMappedSurveyRecordWithId, UUID]()

    logger.info("Retrieving submission records from MongoDB")

    mongoStore.processSurveys(survey_id, Long.MinValue, Long.MaxValue, new Callback1[NutritionMappedSurveyRecordWithId] {
      def call(record: NutritionMappedSurveyRecordWithId) = {
        surveysBuffer += record
      }
    })

    val surveys = surveysBuffer.toSeq

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
      logger.warn("There are no submission records to import")

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
      logger.warn("There are no custom field records to import")

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
      logger.warn("There are no submission user custom field records to import")

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
          logger.warn("There are no meal custom field records to import")

        // Foods

        val mealFoodsParams = meals.flatMap {
          case (meal_id, meal) =>
            meal.foods.map {
              case food =>
                
                logger.debug(food.toString())

                // Some entries use 5-character special food code for missing foods which is no longer allowed

                val corrected_food_code = food.code match {
                  case "$MISS" => "$MIS"
                  case x => x
                }

                Seq[NamedParameter]('meal_id -> meal_id, 'code -> corrected_food_code, 'english_description -> food.englishDescription, 'local_description -> JavaConversions.jopt2option(food.localDescription), 'ready_meal -> food.isReadyMeal, 'search_term -> food.searchTerm,
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
            logger.warn("There are no food custom field records to import")

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
            logger.warn("There are no portion size parameter records to import")

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
            logger.warn("There are no nutrient records to import")

        } else
          logger.warn("There are no food records to import")

      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }

    } else {
      logger.warn("There are no meal records to import")
    }
  }

  def importSurveySubmissions() = {
    val surveys = mongoStore.getSurveyNames()

    surveys.foreach(importSurveySubmissionsFor)
  }

  def importMongoDbData() = {
    importSurveys()
    importUsers()
    importSurveySubmissions()
  }
}

case class Options(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("Intake24 MongoDB to SQL migration tool 16.1-SNAPSHOT")

  val mongoHost = opt[String](required = true, noshort = true)
  val mongoDatabase = opt[String](required = true, noshort = true)
  val mongoUser = opt[String](noshort = true)
  val mongoPassword = opt[String](noshort = true)
  val pgHost = opt[String](required = true, noshort = true)
  val pgDatabase = opt[String](required = true, noshort = true)
  val pgUser = opt[String](required = true, noshort = true)
  val pgPassword = opt[String](noshort = true)
  val pgUseSsl = opt[Boolean](noshort = true)
}

object MongoDbImport extends App {

  val opts = Options(args)

  println("""|=============================================================
              |WARNING: THIS WILL DESTROY ALL EXISTING DATA IN THE DATABASE!
              |=============================================================
              |""".stripMargin)

  var proceed = false;

  val reader = new BufferedReader(new InputStreamReader(System.in))
  
  while (!proceed) {
    println("Are you sure you wish to continue? Type 'yes' to proceed, type 'no' or hit Control-C to exit.")    
    val input = reader.readLine()
    if (input == "yes") proceed = true;
    if (input == "no") System.exit(0);
  }

  val logger = LoggerFactory.getLogger(getClass)

  DriverManager.registerDriver(new org.postgresql.Driver)

  val dataSource = new org.postgresql.ds.PGSimpleDataSource()
  
  dataSource.setServerName(opts.pgHost())
  dataSource.setDatabaseName(opts.pgDatabase())
  dataSource.setUser(opts.pgUser())
  
  opts.pgPassword.foreach(pw => dataSource.setPassword(pw))
  opts.pgUseSsl.foreach(ssl => dataSource.setSsl(ssl))
  
  implicit val dbConn = dataSource.getConnection

  def separateSqlStatements(sql: String) =
    // Regex matches on semicolons that neither precede nor follow other semicolons
    sql.split("(?<!;);(?!;)").map(_.trim.replace(";;", ";")).filterNot(_.isEmpty)

  def stripComments(s: String) = """(?m)/\*(\*(?!/)|[^*])*\*/""".r.replaceAllIn(s, "")
    
  val initDbStatements = separateSqlStatements(stripComments(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/sql/init_system_db.sql"), "utf-8").mkString))

  logger.info("Dropping all tables and sequences")

  val dropTableStatements =
    SQL("""SELECT 'DROP TABLE IF EXISTS ' || tablename || ' CASCADE;' AS query FROM pg_tables WHERE schemaname='public'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val dropSequenceStatements =
    SQL("""SELECT 'DROP SEQUENCE IF EXISTS ' || relname || ' CASCADE;' AS query FROM pg_class WHERE relkind = 'S'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val clearDbStatements = dropTableStatements ++ dropSequenceStatements
  
  clearDbStatements.foreach {
    statement =>
      logger.debug(statement)
      SQL(statement).execute()
  }

  logger.info("Creating tables")

  initDbStatements.foreach { statement =>
    logger.debug(statement)
    SQL(statement).execute()
  }

  val mongoDataStore = new MongoDbDataStore(opts.mongoHost(), 27017, opts.mongoDatabase(), opts.mongoUser.get.getOrElse(""), opts.mongoPassword.get.getOrElse(""))

  val importer = new MongoDbImporter(dbConn, mongoDataStore)

  importer.importMongoDbData()
}