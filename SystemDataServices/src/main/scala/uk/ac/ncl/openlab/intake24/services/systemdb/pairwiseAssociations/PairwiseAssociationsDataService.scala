package uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */

trait PairwiseAssociationsDataService {
  def getAssociations(): Either[UnexpectedDatabaseError, Map[String, PairwiseAssociationRules]]

  def addTransactions(locale: String, transactions: Seq[Seq[String]]): Either[UnexpectedDatabaseError, Unit]
}
