package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsDataService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import anorm.{Macro, SQL}

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */
class PairwiseAssociationsDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends PairwiseAssociationsDataService with SqlDataService {

  private val pairwiseAssociationQuery =
    """
      |SELECT
      |  locale,
      |  antecedent_food,
      |  consequent_food,
      |  occurrences
      |FROM pairwise_associations
      |WHERE locale IN ({locales});
    """.stripMargin

  private case class PairwiseAssociationRow(locale: String, antecedent_food: String, consequent_food: String, occurrences: Int)

  override def getAssociations(locales: Seq[String]): Map[String, PairwiseAssociationRules] = {
    val rows = SQL(pairwiseAssociationQuery).on('locales -> locales).executeQuery().as(Macro.namedParser[PairwiseAssociationRow].*)
    rows.groupBy(_.locale).map { localeNode =>
      localeNode._1 -> localeNode._2.groupBy(_.antecedent_food)
    }
  }

  override def addTransactions(transactions: Seq[Seq[String]]): Unit = ???
}
