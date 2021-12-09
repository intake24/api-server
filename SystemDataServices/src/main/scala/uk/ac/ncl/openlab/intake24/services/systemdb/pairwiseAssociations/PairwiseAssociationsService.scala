package uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations

import uk.ac.ncl.openlab.intake24.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules

import java.util.Calendar
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * Created by Tim Osadchiy on 04/10/2017.
 */

object PairwiseAssociationsServiceSortTypes {
  val popularity = "popularity"
  val paRules = "paRules"

  def invalidAlgorithmId(algorithmId: String) = !Seq(popularity, paRules).contains(algorithmId)

}

case class PairwiseAssociationsServiceConfiguration(minimumNumberOfSurveySubmissions: Int,
                                                    ignoreSurveysContaining: Seq[String],
                                                    useAfterNumberOfTransactions: Int,
                                                    rulesUpdateBatchSize: Int,
                                                    refreshAtTime: String,
                                                    minInputSearchSize: Int,
                                                    readWriteRulesDbBatchSize: Int,
                                                    storedCoOccurrencesThreshold: Int) {

  def nextRefreshIn: FiniteDuration = {
    val parsedParams = refreshAtTime.split(":").map(_.toInt)
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance().getTime()
    cal.setTime(today)
    cal.add(Calendar.DATE, 1)
    cal.set(Calendar.HOUR_OF_DAY, parsedParams(0))
    cal.set(Calendar.MINUTE, parsedParams(1))
    FiniteDuration(cal.getTimeInMillis - today.getTime, TimeUnit.MILLISECONDS)
  }

}

trait PairwiseAssociationsService {

  def getAssociationRules(): Map[String, PairwiseAssociationRules]

  def recommend(locale: String, items: Seq[String], sortType: String, ignoreInputSize: Boolean = false): Seq[(String, Double)]

  def getOccurrences(locale: String): Map[String, Int]

  def update(): Future[Either[DatabaseError, Unit]]

  def rebuild(): Future[Either[DatabaseError, Unit]]
}
