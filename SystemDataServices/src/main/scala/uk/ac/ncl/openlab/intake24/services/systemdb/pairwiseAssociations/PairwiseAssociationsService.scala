package uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations

/**
  * Created by Tim Osadchiy on 04/10/2017.
  */

case class PairwiseAssociationsServiceConfiguration(minimumNumberOfSurveySubmissions: Int,
                                                    ignoreSurveysContaining: Seq[String],
                                                    useAfterNumberOfTransactions: Int,
                                                    rulesUpdateBatchSize: Int)

trait PairwiseAssociationsService {

  def recommend(locale: String, items: Seq[String]): Seq[(String, Double)]

  def addTransactions(surveyId: String, items: Seq[Seq[String]]): Unit

  def refresh(): Unit

}
