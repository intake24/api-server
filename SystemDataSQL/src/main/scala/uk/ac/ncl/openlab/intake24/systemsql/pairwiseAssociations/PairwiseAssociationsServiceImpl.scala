package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import java.time.{ZoneId, ZonedDateTime}
import javax.inject.{Inject, Singleton}

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, SurveyAdminService, SurveyParametersOut}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration}

/**
  * Created by Tim Osadchiy on 04/10/2017.
  */
@Singleton
class PairwiseAssociationsServiceImpl @Inject()(settings: PairwiseAssociationsServiceConfiguration,
                                                dataService: PairwiseAssociationsDataService,
                                                surveyAdminService: SurveyAdminService,
                                                dataExportService: DataExportService) extends PairwiseAssociationsService {

  private val logger = LoggerFactory.getLogger(getClass)

  private var associationRules = getAssociationRules()

  override def recommend(locale: String, items: Seq[String]): Seq[(String, Double)] = associationRules.map { ar =>
    ar.get(locale).map { rules =>
      val params = rules.getParams()
      if (params.numberOfTransactions < settings.minimumNumberOfSurveySubmissions || items.isEmpty) {
        params.occurrences.map(o => o._1 -> o._2.toDouble).toSeq.sortBy(-_._2)
      } else {
        rules.recommend(items)
      }
    }.getOrElse(Nil)
  }.getOrElse(Nil)

  override def addTransactions(surveyId: String, items: Seq[Seq[String]]): Unit = getValidSurvey(surveyId, "addTransactions").map { surveyParams =>
    associationRules.map { localeAr =>
      localeAr.get(surveyParams.localeId) match {
        case None =>
          val ar = PairwiseAssociationRules(None)
          ar.addTransactions(items)
          associationRules = Right(localeAr + (surveyParams.localeId -> ar))
        case Some(ar) => ar.addTransactions(items)
      }
      dataService.addTransactions(surveyParams.localeId, items)
    }
  }

  override def refresh(): Unit = {
    val foldGraph = Map[String, PairwiseAssociationRules]().withDefaultValue(PairwiseAssociationRules(None))
    val graph = surveyAdminService.listSurveys().getOrElse(Nil)
      .foldLeft(foldGraph) { (foldGraph, survey) =>
        getSurveySubmissions(survey).foldLeft(foldGraph) { (foldGraph, submission) =>
          val localeRules = foldGraph(submission.locale)
          localeRules.addTransactions(submission.meals)
          foldGraph + (submission.locale -> localeRules)
        }
      }
    dataService.writeAssociations(graph) match {
      case Left(e) => logger.error(s"Failed to refresh PairwiseAssociations ${e.exception.getMessage}")
      case Right(_) => associationRules = getAssociationRules()
    }
  }

  private def getSurveySubmissions(survey: SurveyParametersOut): Seq[Submission] = {
    val submissionCount = getSubmissionCount(survey.id)
    if (surveyIsValid(survey.id, submissionCount, "getSurveySubmissions")) {
      Range(0, submissionCount, settings.rulesUpdateBatchSize).foldLeft(Seq[Submission]()) { (submissions, offset) =>
        submissions ++ dataExportService.getSurveySubmissions(survey.id, None, None, offset, settings.rulesUpdateBatchSize, None)
          .map { exportSubmissions =>
            exportSubmissions.map { expSubmission =>
              val meals = expSubmission.meals.map { meal => meal.foods.map(_.code) }
              Submission(survey.localeId, meals)
            }
          }.getOrElse(Nil)
      }
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

  private def getAssociationRules() = {
    val associationRules = dataService.getAssociations()
    associationRules match {
      case Left(dbError) => logger.error(dbError.exception.getMessage)
    }
    associationRules
  }

  private case class Submission(locale: String, meals: Seq[Seq[String]])

}
