package uk.ac.ncl.openlab.intake24.sql.tools.system

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools._
import uk.ac.ncl.openlab.intake24.systemsql.admin.{DataExportImpl, SurveyAdminImpl}
import uk.ac.ncl.openlab.intake24.systemsql.user.FoodPopularityServiceImpl

import scala.collection.mutable

object ResetPopularityCounters extends App with DatabaseConnection with WarningMessage with ErrorHandler {

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())

  val dataSource = getDataSource(databaseConfig)

  val surveyAdminService = new SurveyAdminImpl(dataSource)
  val dataExportService = new DataExportImpl(dataSource)
  val foodPopularityService = new FoodPopularityServiceImpl(dataSource)

  val surveys = throwOnError(surveyAdminService.listSurveys())

  val counters = mutable.Map[String, Int]().withDefaultValue(0)

  surveys.foreach {
    survey =>
      println(s"Processing survey ${survey.id}")

      val dateFrom = ZonedDateTime.now().minus(10, ChronoUnit.YEARS)
      val dateTo = ZonedDateTime.now()

      val submissionsCount = throwOnError(dataExportService.getSurveySubmissionCount(survey.id, dateFrom, dateTo))

      val batchSize = 50

      Range(0, submissionsCount, batchSize).foreach {
        offset =>

          val submissions = throwOnError(dataExportService.getSurveySubmissions(survey.id, Some(dateFrom), Some(dateTo), offset, batchSize, None))

          submissions.foreach {
            submission =>
              submission.meals.foreach {
                meal =>
                  meal.foods.foreach {
                    food =>
                      counters.update(food.code, counters(food.code) + 1)
                  }
              }
          }

          println(s"  processed ${offset + submissions.size} out of $submissionsCount")
      }
  }

  println()
  println()

  println("Updating counters...")

  throwOnError(foodPopularityService.setPopularityCounters(counters.toMap))

  println("Done!")
}
