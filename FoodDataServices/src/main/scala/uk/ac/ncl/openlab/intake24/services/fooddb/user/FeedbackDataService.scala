package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError


case class FiveADayFeedbackRow(ifLessThan: Int, sentiment: String, summary: String, feedback: String)

trait FeedbackDataService {
  def getFiveADayFeedback(): Either[UnexpectedDatabaseError, Seq[FiveADayFeedbackRow]]
}
