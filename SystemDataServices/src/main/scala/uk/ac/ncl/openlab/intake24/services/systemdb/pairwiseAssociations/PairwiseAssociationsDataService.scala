package uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations

import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */

trait PairwiseAssociationsDataService {
  def getAssociations(locales: Seq[String]): Map[String, PairwiseAssociationRules]

  def addTransactions(transactions: Seq[Seq[String]])
}
