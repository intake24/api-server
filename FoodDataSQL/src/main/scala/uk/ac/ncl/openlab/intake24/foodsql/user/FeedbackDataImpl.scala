package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.inject.{Inject, Named, Singleton}
import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{FeedbackDataService, FiveADayFeedbackRow}
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import anorm.{Macro, SQL}


@Singleton
class FeedbackDataImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends FeedbackDataService with SqlDataService {
  override def getFiveADayFeedback(): Either[UnexpectedDatabaseError, Seq[FiveADayFeedbackRow]] = tryWithConnection {
    implicit connection =>
      Right(SQL("SELECT if_less_than, sentiment, summary, feedback FROM five_a_day_feedback ORDER BY if_less_than DESC")
        .executeQuery()
        .as(Macro.namedParser[FiveADayFeedbackRow](Macro.ColumnNaming.SnakeCase).*))
  }
}
