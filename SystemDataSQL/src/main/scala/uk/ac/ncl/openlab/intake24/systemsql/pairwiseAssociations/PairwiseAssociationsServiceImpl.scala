package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import java.time.{ZoneId, ZonedDateTime}

import javax.inject.{Inject, Named, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.UpdateError
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, ExportSubmission, SurveyAdminService, SurveyParametersOut}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration, PairwiseAssociationsServiceSortTypes}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by Tim Osadchiy on 04/10/2017.
  */

@Singleton
class PairwiseAssociationsServiceImpl @Inject()(settings: PairwiseAssociationsServiceConfiguration,
                                                dataService: PairwiseAssociationsDataService,
                                                surveyAdminService: SurveyAdminService,
                                                dataExportService: DataExportService,
                                                @Named("intake24") implicit val executionContext: ExecutionContext) extends PairwiseAssociationsService {

  type LocalizedPARules = Map[String, PairwiseAssociationRules]

  private val threadSleepFor = 5

  private val logger = LoggerFactory.getLogger(getClass)

  private var associationRules = Map[String, PairwiseAssociationRules]()

  private val associationRulesPromise = Promise[Map[String, PairwiseAssociationRules]]

  getAssociationRulesFromDb()

  override def getAssociationRules(): Map[String, PairwiseAssociationRules] = associationRules

  override def getAssociationRulesAsync(): Future[Map[String, PairwiseAssociationRules]] = associationRulesPromise.future

  override def recommend(locale: String,
                         items: Seq[String],
                         sortType: String = PairwiseAssociationsServiceSortTypes.paRules,
                         ignoreInputSize: Boolean = false): Seq[(String, Double)] = extractPairwiseRules(locale) { rules =>
    val params = rules.getParams()

    if (params.numberOfTransactions < settings.minimumNumberOfSurveySubmissions ||
      (items.size < settings.minInputSearchSize && !ignoreInputSize) ||
      sortType == PairwiseAssociationsServiceSortTypes.popularity) {
      params.occurrences.map(o => o._1 -> o._2.toDouble).toSeq.sortBy(-_._2)
    } else {
      rules.recommend(items)
    }
  }.getOrElse(Nil)

  override def getOccurrences(locale: String): Map[String, Int] = {
    extractPairwiseRules(locale) { rules =>
      rules.getParams().occurrences
    }.getOrElse(Map[String, Int]())
  }

  override def addTransactions(surveyId: String, items: Seq[Seq[String]]): Unit =
    getValidSurvey(surveyId, "addTransactions").map { surveyParams =>
      associationRules.get(surveyParams.localeId) match {
        case None =>
          val ar = PairwiseAssociationRules(None)
          ar.addTransactions(items)
          associationRules = associationRules + (surveyParams.localeId -> ar)
        case Some(ar) => ar.addTransactions(items)
      }
      dataService.addTransactions(surveyParams.localeId, items)
    }

  override def refresh(): Future[Either[UpdateError, LocalizedPARules]] = {
    for (
      graph <- buildRules();
      result <- dataService.writeAssociations(graph)
    ) yield {
      result match {
        case Left(e) =>
          logger.error(s"Failed to refresh PairwiseAssociations ${e.exception.getMessage}")
          Left(e)
        case Right(_) =>
          logger.debug(s"Successfully refreshed Pairwise associations")
          this.associationRules = graph
          Right(graph)
      }
    }
  }

  def buildRules(): Future[LocalizedPARules] = {
    Future {
      logger.debug("Refreshing Pairwise associations")
      logger.debug("Collecting surveys")
      val surveys = surveyAdminService.listSurveys()
      surveys.left.foreach(e => logger.error(e.exception.getMessage))

      logger.debug("Building new pairwise associations graph")
      val foldGraph = Map[String, PairwiseAssociationRules]().withDefault(_ => PairwiseAssociationRules(None))
      surveys.getOrElse(Nil).foldLeft(foldGraph) { (foldGraph, survey) =>
        val submissions = getSurveySubmissions(survey)

        logger.debug("Building a graph")

        val localeRules = foldGraph(survey.localeId)
        val transactions = submissions.flatMap(_.meals.map(_.foods.map(_.code)))
        localeRules.addTransactions(transactions)
        foldGraph + (survey.localeId -> localeRules)
      }

    }
  }

  private def extractPairwiseRules[T](localeId: String)(f: PairwiseAssociationRules => T): Option[T] =
    associationRules.get(localeId).map(rules => f(rules))

  private def getSurveySubmissions(survey: SurveyParametersOut): Seq[ExportSubmission] = {
    val submissionCount = getSubmissionCount(survey.id)
    if (surveyIsValid(survey.id, submissionCount, "getSurveySubmissions")) {
      logger.debug(s"Retrieving $submissionCount submissions from survey ${survey.id}")
      val submissions = Range(0, submissionCount, settings.rulesUpdateBatchSize).foldLeft(Seq[ExportSubmission]()) { (submissions, offset) =>
        submissions ++ dataExportService.getSurveySubmissions(survey.id, None, None, offset, settings.rulesUpdateBatchSize, None).getOrElse(Nil)
      }
      submissions
    } else {
      Nil
    }
  }

  private def getValidSurvey(surveyId: String, operationPrefix: String): Option[SurveyParametersOut] =
    if (surveyIsValid(surveyId, getSubmissionCount(surveyId), operationPrefix)) {
      surveyAdminService.getSurvey(surveyId) match {
        case Left(e) =>
          logger.error(e.exception.getMessage)
          None
        case Right(surveyParametersOut) => Some(surveyParametersOut)
      }
    } else {
      None
    }


  private def surveyIsValid(surveyId: String, submissionCount: Int, operationPrefix: String): Boolean =
    if (settings.ignoreSurveysContaining.exists(stopWord => surveyId.contains(stopWord))) {
      logger.warn(s"Survey $surveyId is ignored due to its' name at $operationPrefix")
      false
    } else if (submissionCount < settings.minimumNumberOfSurveySubmissions) {
      logger.warn(s"Survey $surveyId is ignored since it contains less than ${settings.minimumNumberOfSurveySubmissions} at $operationPrefix")
      false
    } else {
      true
    }

  private def getSubmissionCount(surveyId: String) = {
    val dateFrom = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
    val dateTo = ZonedDateTime.now()
    dataExportService.getSurveySubmissionCount(surveyId, dateFrom, dateTo).getOrElse(0)
  }

  private def getAssociationRulesFromDb() = dataService.getAssociations().onComplete({
    case Success(mp) =>
      this.associationRules = mp
      associationRulesPromise.success(mp)
    case Failure(e) =>
      logger.error(e.getMessage)
      associationRulesPromise.failure(e)
  })

  private case class Submission(locale: String, meals: Seq[Seq[String]])

}
