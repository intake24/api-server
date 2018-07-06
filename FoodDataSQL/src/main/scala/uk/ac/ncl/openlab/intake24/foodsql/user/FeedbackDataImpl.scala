package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.inject.{Inject, Named, Singleton}
import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{FeedbackDataService, FiveADayFeedbackRow, FoodGroupFeedbackRow, FoodGroupValueThreshold}
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import anorm.{Macro, SQL, SqlParser, ~}

@Singleton
class FeedbackDataImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends FeedbackDataService with SqlDataService {


  private case class InternalRow(id: Int, name: String, tooHighThreshold: Option[Double], tooHighMessage: Option[String],
                                 tooLowThreshold: Option[Double], tooLowMessage: Option[String], tellMeMoreText: String)

  override def getFiveADayFeedback(): Either[UnexpectedDatabaseError, Seq[FiveADayFeedbackRow]] = tryWithConnection {
    implicit connection =>
      Right(SQL("SELECT if_less_than, feedback FROM five_a_day_feedback ORDER BY if_less_than DESC")
        .executeQuery()
        .as(Macro.namedParser[FiveADayFeedbackRow](Macro.ColumnNaming.SnakeCase).*))
  }

  def getFoodGroupsFeedback(): Either[UnexpectedDatabaseError, Seq[FoodGroupFeedbackRow]] = tryWithConnection {
    implicit connection =>

      val foodGroups = SQL("SELECT food_groups_feedback_id, food_group_id FROM food_groups_feedback_group_ids")
        .executeQuery()
        .as((SqlParser.int(1) ~ SqlParser.int(2)).*)
        .foldLeft(Map[Int, Seq[Int]]().withDefaultValue(Seq())) {
          case (m, feedback_id ~ food_group_id) => m + (feedback_id -> (food_group_id +: m(feedback_id)))
        }

      val rows = SQL("SELECT id, name, too_high_threshold, too_high_message, too_low_threshold, too_low_message, tell_me_more_text FROM food_groups_feedback")
        .executeQuery()
        .as(Macro.namedParser[InternalRow](Macro.ColumnNaming.SnakeCase).*)


      val result = rows.map {
        row =>

          val low = for (threshold <- row.tooLowThreshold;
                         message <- row.tooLowMessage) yield FoodGroupValueThreshold(threshold, message)

          val high = for (threshold <- row.tooHighThreshold;
                          message <- row.tooHighMessage) yield FoodGroupValueThreshold(threshold, message)


          FoodGroupFeedbackRow(row.name, foodGroups.getOrElse(row.id, Seq()), low, high, row.tellMeMoreText)
      }

      Right(result)
  }
}
