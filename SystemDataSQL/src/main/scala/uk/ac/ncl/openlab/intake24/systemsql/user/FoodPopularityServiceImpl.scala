package uk.ac.ncl.openlab.intake24.systemsql.user

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.FoodPopularityService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class FoodPopularityServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends FoodPopularityService with SqlDataService with SqlResourceLoader {

  def getPopularityCount(foodCodes: Seq[String]): Either[UnexpectedDatabaseError, Map[String, Int]] = tryWithConnection {
    implicit conn =>
      if (foodCodes.isEmpty)
        Right(Map())
      else {
        val counters = SQL("SELECT food_code, counter FROM popularity_counters WHERE food_code IN ({food_codes})")
          .on('food_codes -> foodCodes)
          .executeQuery()
          .as((SqlParser.str("food_code") ~ SqlParser.int("counter")).*)
          .map {
            case food_code ~ counter => food_code -> counter
          }
          .toMap

        Right(foodCodes.map(code => (code, counters.getOrElse(code, 0))).toMap)
      }
  }

  def incrementPopularityCount(foodCodes: Seq[String]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      val updateParams = foodCodes.map(code => Seq[NamedParameter]('food_code -> code))

      if (!updateParams.isEmpty) {
        BatchSql("INSERT INTO popularity_counters(food_code, counter) VALUES ({food_code},0) ON CONFLICT(food_code) DO UPDATE SET counter=popularity_counters.counter+1", updateParams.head, updateParams.tail: _*).execute()
      }

      Right(())
  }

  def setPopularityCounters(counters: Map[String, Int]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      val updateParams = counters.toSeq.map {
        case (code, count) => Seq[NamedParameter]('food_code -> code, 'counter -> count)
      }

      if (!updateParams.isEmpty) {
        BatchSql("UPDATE popularity_counters SET counter={counter} WHERE food_code={food_code}", updateParams.head, updateParams.tail: _*).execute()
      }

      Right(())
  }

}
