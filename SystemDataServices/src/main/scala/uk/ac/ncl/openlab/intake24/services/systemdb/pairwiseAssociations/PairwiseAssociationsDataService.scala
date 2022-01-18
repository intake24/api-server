package uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations

import uk.ac.ncl.openlab.intake24.errors.{DatabaseError, UnexpectedDatabaseError, UpdateError}
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules

import java.time.ZonedDateTime
import scala.concurrent.{Future, Promise}

/**
 * Created by Tim Osadchiy on 02/10/2017.
 */

trait PairwiseAssociationsDataService {

  def getLastSubmissionTime(): Either[UnexpectedDatabaseError, ZonedDateTime]

  def updateLastSubmissionTime(time: ZonedDateTime): Either[UpdateError, Unit]

  def getAssociations(): Future[Either[DatabaseError, Map[String, PairwiseAssociationRules]]]

  def writeAssociations(localeAssociations: Map[String, PairwiseAssociationRules]): Future[Either[UpdateError, Unit]]

  def addTransactions(locale: String, transactions: Seq[Seq[String]]): Either[UpdateError, Unit]

  def copyOccurrenceData(sourceLocale: String, destinationLocale: String): Future[Either[DatabaseError, Unit]]

  def copyCoOccurrenceData(sourceLocale: String, destinationLocale: String): Future[Either[DatabaseError, Unit]]
}
