package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsDataService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import anorm.{Macro, SQL}
import uk.ac.ncl.openlab.intake24.errors.RecordNotFound

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */
class PairwiseAssociationsDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends PairwiseAssociationsDataService with SqlDataService {

  private val pairwiseAssociationQuery =
    """
      |SELECT
      |  locale,
      |  antecedent_food_code,
      |  consequent_food_code,
      |  occurrences
      |FROM pairwise_associations_co_occurrences
      |WHERE locale IN ({locales});
    """.stripMargin

  private val occurrencesQuery =
    """
      |SELECT
      |  locale,
      |  food_code,
      |  occurrences
      |FROM pairwise_associations_occurrences
      |WHERE locale IN ({locales});
    """.stripMargin

  private case class PairwiseAssociationRow(locale: String, antecedent_food_code: String, consequent_food_code: String, occurrences: Int)

  private case class OccurrenceRow(locale: String, food_code: String, occurrences: Int)

  override def getAssociations(locales: Seq[String]): Map[String, PairwiseAssociationRules] = tryWithConnection {
    implicit conn =>
      SQL(pairwiseAssociationQuery).on('locales -> locales).executeQuery().as(Macro.namedParser[PairwiseAssociationRow].*) match {
        case Nil => Left(RecordNotFound(new RuntimeException("No records were found for")))
        case rows => rows.groupBy(_.locale).map { localeNode =>
          localeNode._1 -> localeNode._2.groupBy(_.antecedent_food)
        }
      }
  }
  override def addTransactions(transactions: Seq[Seq[String]]): Unit = ???
}
