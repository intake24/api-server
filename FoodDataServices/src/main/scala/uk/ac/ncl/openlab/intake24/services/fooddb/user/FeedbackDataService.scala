package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError

case class FiveADayFeedbackRow(ifLessThan: Int, feedback: String)

case class FoodGroupValueThreshold(threshold: Double, message: String)

case class FoodGroupFeedbackRow(name: String, foodGroupIds: Seq[Int], low: Option[FoodGroupValueThreshold], high: Option[FoodGroupValueThreshold], tellMeMore: String)


trait FeedbackDataService {
  def getFiveADayFeedback(): Either[UnexpectedDatabaseError, Seq[FiveADayFeedbackRow]]
  def getFoodGroupsFeedback(): Either[UnexpectedDatabaseError, Seq[FoodGroupFeedbackRow]]
}
