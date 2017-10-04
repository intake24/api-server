package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import javax.inject.{Inject, Singleton}

import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration}

/**
  * Created by Tim Osadchiy on 04/10/2017.
  */
@Singleton
class PairwiseAssociationsServiceImpl @Inject()(settings: PairwiseAssociationsServiceConfiguration, dataService: PairwiseAssociationsDataService) extends PairwiseAssociationsService {

  override def recommend(locale: String, items: Seq[String]): Seq[(String, Double)] = ???

  override def addTransactions(locale: String, items: Seq[Seq[String]]): Unit = ???

}
