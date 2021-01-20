package uk.ac.ncl.openlab.intake24.sql.tools.system

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.data.UserFoodHeader
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodBrowsingServiceImpl
import uk.ac.ncl.openlab.intake24.sql.tools._
import uk.ac.ncl.openlab.intake24.surveydata._
import uk.ac.ncl.openlab.intake24.systemsql.user.SurveyServiceImpl

import java.time.ZonedDateTime
import java.util.UUID
import scala.language.reflectiveCalls
import scala.util.Random

object GenerateRandomSubmissions extends App with DatabaseConnection with WarningMessage with ErrorHandler {

  private val random = new Random()
  private val alphabet = ('0' to '9').mkString + ('A' to 'Z').mkString + ('a' to 'z').mkString

  def generateRandomString(length: Int): String = {
    val sb = new StringBuffer()

    1.to(length).foreach(_ => sb.append(alphabet.charAt(random.nextInt(alphabet.length))))

    sb.toString
  }

  val numberOfMeals = Range(1, 6)
  val numberOfFoods = Range(2, 8)


  def chooseRandomFood(allFoods: Seq[UserFoodHeader]): UserFoodHeader = allFoods(Random.nextInt(allFoods.size))

  def generateRandomFoods(number: Int, allFoods: Seq[UserFoodHeader]): Seq[NutrientMappedFood] = {
    Range(0, number).map {
      _ =>

        val food = chooseRandomFood(allFoods)

        NutrientMappedFood(
          food.code,
          food.localDescription,
          food.localDescription,
          false,
          generateRandomString(10),
          generateRandomString(10),
          PortionSizeWithWeights(
            100.0,
            0.0,
            100.0,
            "mock",
            Map("servingWeight" -> "100.0", "leftoversWeight" -> "100.0")
          ),
          Map(),
          Some("NDNS_Yr12"),
          Some("1"),
          true,
          1,
          "Blah",
          None,
          Map(),
          Map()
        )
    }
  }

  def generateRandomMeals(number: Int, allFoods: Seq[UserFoodHeader]): Seq[NutrientMappedMeal] = {
    Range(0, number).map {
      _ =>
        NutrientMappedMeal(
          generateRandomString(10),
          MealTime(0, 0),
          Map(),
          generateRandomFoods(random.nextInt(numberOfFoods.`end` - numberOfFoods.start) + numberOfFoods.start, allFoods),
          Seq()
        )
    }
  }

  def generateSubmission(userId: Int, allFoods: Seq[UserFoodHeader]): NutrientMappedSubmission =
    NutrientMappedSubmission(
      ZonedDateTime.now(),
      ZonedDateTime.now(),
      UUID.randomUUID(),
      generateRandomMeals(random.nextInt(numberOfMeals.`end` - numberOfMeals.start) + numberOfFoods.start, allFoods),
      Map()
    )


  val options = new ScallopConf(args) {
    val systemDbConfig = opt[String](required = true)
    val foodsDbConfig = opt[String](required = true)
    val localeId = opt[String](required = true)
    val surveyId = opt[String](required = true)
    val userId = opt[Int](required = true)
    val number = opt[Int](required = true)
  }

  options.verify()

  val systemDataSource = getDataSource(DatabaseConfigChooser.chooseDatabaseConfiguration(options.systemDbConfig()))
  val foodsDataSource = getDataSource(DatabaseConfigChooser.chooseDatabaseConfiguration(options.foodsDbConfig()))

  val surveyService = new SurveyServiceImpl(systemDataSource)
  val foodsService = new FoodBrowsingServiceImpl(foodsDataSource)

  val localeId = options.localeId()
  val userId = options.userId()
  val surveyId = options.surveyId()

  println(s"Retrieving foods list for locale $localeId...")
  val allFoods = throwOnError(foodsService.listAllFoods(localeId))

  Range(0, options.number()).foreach {
    n =>
      val submission = generateSubmission(userId, allFoods)
      throwOnError(surveyService.createSubmission(userId, surveyId, submission))
  }

  println("Done!")
}
