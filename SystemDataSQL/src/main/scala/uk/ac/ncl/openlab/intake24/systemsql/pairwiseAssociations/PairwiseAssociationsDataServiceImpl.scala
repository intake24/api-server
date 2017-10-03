package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.{PairwiseAssociationRules, PairwiseAssociationRulesConstructorParams}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsDataService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import anorm.{Macro, SQL}
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound, UnexpectedDatabaseError}

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */
class PairwiseAssociationsDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends PairwiseAssociationsDataService with SqlDataService {

  private val coOccurrenceQuery = "SELECT * FROM pairwise_associations_co_occurrences;"

  private val occurrencesQuery = "SELECT * FROM pairwise_associations_occurrences;"

  private val transactionsCountQuery = "SELECT * FROM pairwise_associations_transactions_count;"

  private case class CoOccurrenceRow(locale: String, antecedent_food_code: String, consequent_food_code: String, occurrences: Int)

  private case class OccurrenceRow(locale: String, food_code: String, occurrences: Int)

  private case class TransactionCountRow(locale: String, transactions_count: Int)

  override def getAssociations(): Either[UnexpectedDatabaseError, Map[String, PairwiseAssociationRules]] = {
    for (
      transactionCounts <- getTransactionCounts();
      occurrences <- getOccurrenceMap();
      coOccurrences <- getCoOccurrenceMap();
      associationRules = transactionCounts.flatMap { transactionNode =>
        for (
          occurrenceMap <- occurrences.get(transactionNode._1);
          coOccurrenceMap <- coOccurrences.get(transactionNode._1);
          paramsNode = transactionNode._1 -> {
            val params = PairwiseAssociationRulesConstructorParams(transactionNode._2, occurrenceMap, coOccurrenceMap)
            PairwiseAssociationRules(Some(params))
          };
        ) yield paramsNode
      }
    ) yield associationRules
  }

  override def addTransactions(locale: String, transactions: Seq[Seq[String]]): Unit = ???

  private def getCoOccurrenceMap(): Either[UnexpectedDatabaseError, Map[String, Map[String, Map[String, Int]]]] = tryWithConnection {
    implicit conn =>
      val mp = SQL(coOccurrenceQuery).executeQuery().as(Macro.namedParser[CoOccurrenceRow].*).groupBy(_.locale).map { localeNode =>
        localeNode._1 -> localeNode._2.groupBy(_.antecedent_food_code).map { antNode =>
          antNode._1 -> antNode._2.groupBy(_.consequent_food_code).map { consNode =>
            consNode._1 -> consNode._2.head.occurrences
          }
        }
      }
      Right(mp)
  }

  private def getOccurrenceMap(): Either[UnexpectedDatabaseError, Map[String, Map[String, Int]]] = tryWithConnection {
    implicit conn =>
      val mp = SQL(occurrencesQuery).executeQuery().as(Macro.namedParser[OccurrenceRow].*).groupBy(_.locale).map { n =>
        n._1 -> n._2.groupBy(_.food_code).map { fn => fn._1 -> fn._2.head.occurrences }
      }
      Right(mp)
  }

  private def getTransactionCounts(): Either[UnexpectedDatabaseError, Map[String, Int]] = tryWithConnection {
    implicit conn =>
      val mp = SQL(transactionsCountQuery).executeQuery().as(Macro.namedParser[TransactionCountRow].*).groupBy(_.locale).map { n =>
        n._1 -> n._2.head.transactions_count
      }
      Right(mp)
  }

}
