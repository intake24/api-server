package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError

case class FiveADayFeedback(tellMeMoreText: String, tooLowMessage: String)

case class FoodGroupValueThreshold(threshold: Double, message: String)

case class FoodGroupFeedbackRow(name: String, nutrientIds: Seq[Int], low: Option[FoodGroupValueThreshold], high: Option[FoodGroupValueThreshold], tellMeMore: String)

trait FeedbackDataService {
  def getFiveADayFeedback(): Either[UnexpectedDatabaseError, FiveADayFeedback]
  def getFoodGroupsFeedback(): Either[UnexpectedDatabaseError, Seq[FoodGroupFeedbackRow]]
}
