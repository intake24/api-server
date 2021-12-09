package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import cats.data.EitherT
import cats.implicits._

import java.time.{Instant, ZoneId, ZonedDateTime}
import javax.inject.{Inject, Named, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{DatabaseError, ErrorUtils, UnexpectedDatabaseError, UpdateError}
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, ExportSubmission, SurveyAdminService, SurveyParametersOut}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration, PairwiseAssociationsServiceSortTypes}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}


/**
 * Created by Tim Osadchiy on 04/10/2017.
 */

case class Transactions(lastSubmissionTime: ZonedDateTime, batches: List[TransactionBatch])

case class TransactionBatch(localeId: String, transactions: Seq[Seq[String]])

@Singleton
class PairwiseAssociationsServiceImpl @Inject()(settings: PairwiseAssociationsServiceConfiguration,
                                                dataService: PairwiseAssociationsDataService,
                                                surveyAdminService: SurveyAdminService,
                                                dataExportService: DataExportService,
                                                @Named("intake24") implicit val executionContext: ExecutionContext) extends PairwiseAssociationsService {

  type LocalizedPARules = Map[String, PairwiseAssociationRules]

  private val dateLowerBound = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());

  private val threadSleepFor = 5

  private val logger = LoggerFactory.getLogger(getClass)

  private var associationRules = Map[String, PairwiseAssociationRules]()

  getAssociationRulesFromDb()

  override def getAssociationRules(): Map[String, PairwiseAssociationRules] = associationRules

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

  private def addTransactions(transactions: List[TransactionBatch]): Future[Either[DatabaseError, Unit]] = Future {
    val grouped = groupTransactions(transactions)
    ErrorUtils.sequence(grouped.toSeq.map { case (localeId, transactions) => dataService.addTransactions(localeId, transactions) }).right.map(_ => ())
  }

  // TODO: Use atomic reference
  private def updateRules(newRules: Map[String, PairwiseAssociationRules]): Unit = this.associationRules = newRules

  override def update(): Future[Either[DatabaseError, Unit]] = {

    val eitherT = for (
      lastSubmissionTime <- EitherT(Future.successful(dataService.getLastSubmissionTime()));
      transactions <- EitherT(collectTransactionsAfter(lastSubmissionTime));
      _ <- EitherT(addTransactions(transactions.batches));
      _ <- EitherT(Future.successful(dataService.updateLastSubmissionTime(transactions.lastSubmissionTime)));
      associations <- EitherT(dataService.getAssociations())
    ) yield updateRules(associations)

    eitherT.value
  }

  override def rebuild(): Future[Either[DatabaseError, Unit]] = {
    val eitherT = for (
      transactions <- EitherT(collectTransactions());
      graph <- EitherT(Future.successful(Right(buildRules(transactions.batches))));
      _ <- EitherT(dataService.writeAssociations(graph));
      _ <- EitherT(Future.successful(dataService.updateLastSubmissionTime(transactions.lastSubmissionTime)))
    ) yield updateRules(graph)

    eitherT.value
  }

  @tailrec
  private def processSubmissions(surveyId: String, total: Int, offset: Int = 0)(action: Seq[ExportSubmission] => Unit): Either[DatabaseError, Unit] = {
    logger.debug(s"Fetching next batch of up to ${settings.rulesUpdateBatchSize} submissions at offset $offset (expected total $total)")

    val t0 = System.currentTimeMillis()

    val result = dataExportService.getSurveySubmissions(surveyId, None, None, offset, settings.rulesUpdateBatchSize, None)

    logger.debug(s"Received new batch in ${System.currentTimeMillis() - t0} ms")

    result match {
      case Left(error) =>
        logger.warn(s"Failed to retrieve the next batch of submissions for survey $surveyId, offset $offset, batch size ${settings.rulesUpdateBatchSize}", error.exception)
        Left(error)

      case Right(submissions) =>
        if (submissions.nonEmpty) {
          logger.debug(s"Processing ${submissions.size} submissions")
          val t0 = System.currentTimeMillis()
          action(submissions)
          logger.debug(s"Processed current batch in ${System.currentTimeMillis() - t0} ms")
          processSubmissions(surveyId, total, offset + submissions.size)(action)
        } else Right(())
    }
  }

  def collectTransactions(): Future[Either[UnexpectedDatabaseError, Transactions]] = collectTransactionsAfter(dateLowerBound)

  def collectTransactionsAfter(after: ZonedDateTime): Future[Either[UnexpectedDatabaseError, Transactions]] = Future {
    logger.debug(s"Collecting transactions from surveys after $after")

    surveyAdminService.listSurveys().flatMap {
      surveys =>
        val batches = mutable.MutableList()

        var lastSubmissionTime = after

        surveys.foreach {
          surveyParams =>

            val submissionCount = getSubmissionCount(surveyParams.id)

            if (surveyIsValid(surveyParams.id, submissionCount, "getSurveySubmissions")) {
              logger.debug(s"Processing submissions from survey ${surveyParams.id}")

              processSubmissions(surveyParams.id, submissionCount) {
                submissions =>
                  implicit val localDateOrdering: Ordering[ZonedDateTime] = _ compareTo _
                  lastSubmissionTime = localDateOrdering.max(lastSubmissionTime, submissions.map(_.submissionTime).max)
                  val surveyTransactions = submissions.flatMap(_.meals.map(_.foods.map(_.code)))
                  batches += TransactionBatch(surveyParams.localeId, surveyTransactions)
              }
            }
        }

        logger.debug(s"Last seen submission time: $lastSubmissionTime")
        Right(Transactions(lastSubmissionTime, batches.toList))
    }
  }

  def groupTransactions(transactions: List[TransactionBatch]): Map[String, Seq[Seq[String]]] =
    transactions.groupBy(_.localeId).map { case (k, v) => (k, v.flatMap(_.transactions)) }

  def buildRules(transactions: Seq[TransactionBatch]): LocalizedPARules = {
    logger.debug(s"Building new pairwise associations graph from ${transactions.size} transaction batches")
    val z = Map[String, PairwiseAssociationRules]().withDefault(_ => PairwiseAssociationRules(None))
    transactions.foldLeft(z) { (map, transactionsBatch) =>
      logger.debug(s"Processing a transaction batch of size ${transactionsBatch.transactions.size}")
      val localeRules = map(transactionsBatch.localeId)
      localeRules.addTransactions(transactionsBatch.transactions)
      map + (transactionsBatch.localeId -> localeRules)
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
    case Success(Right(mp)) =>
      updateRules(mp)
    case Success(Left(error)) =>
      logger.error(error.exception.getMessage)
    case Failure(e) =>
      logger.error(e.getMessage)
  })

  private case class Submission(locale: String, meals: Seq[Seq[String]])

}
