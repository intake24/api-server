package uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations

import uk.ac.ncl.openlab.intake24.errors.{UnexpectedDatabaseError, UpdateError}
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules

import scala.concurrent.{Future, Promise}

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */

trait PairwiseAssociationsDataService {

  def getAssociations(): Future[Map[String, PairwiseAssociationRules]]

  def writeAssociations(localeAssociations: Map[String, PairwiseAssociationRules]): Future[Either[UpdateError, Unit]]

  def addTransactions(locale: String, transactions: Seq[Seq[String]]): Either[UnexpectedDatabaseError, Unit]

}
