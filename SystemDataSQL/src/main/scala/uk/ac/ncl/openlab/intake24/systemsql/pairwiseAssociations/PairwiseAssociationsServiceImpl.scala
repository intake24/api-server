package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import java.time.{ZoneId, ZonedDateTime}
import java.time.temporal.ChronoUnit
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
    ar.get(locale).flatMap(_.recommend(items)).toSeq
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

    val graph = surveyAdminService.listSurveys().getOrElse(Nil).map { survey =>
      survey.id -> getSubmissionCount(survey.id)
    }.foldLeft(foldGraph) { (gr, survey) =>
      if (surveyIsValid(survey._1, survey._2, "refresh")) {
        Range(0, survey._2, settings.rulesUpdateBatchSize).foldLeft(gr) { (ocMp, offset) =>
          val subs = dataExportService.getSurveySubmissions(survey._1, None, None, offset, settings.rulesUpdateBatchSize, None)
          println(s"  processed ${offset + subs.size} out of $submissionCount")
          subs
        }
      }
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
      logger.warn(s"$operationPrefix - survey $surveyId is ignored due to its' name")
      false
    } else if (submissionCount < settings.minimumNumberOfSurveySubmissions) {
      logger.warn(s"$operationPrefix - survey $surveyId is ignored since it contains less than ${settings.minimumNumberOfSurveySubmissions}")
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
    val associationRules = dataService.getAssociationsByLocale()
    associationRules match {
      case Left(dbError) => logger.error(dbError.exception.getMessage)
    }
    associationRules
  }

}
