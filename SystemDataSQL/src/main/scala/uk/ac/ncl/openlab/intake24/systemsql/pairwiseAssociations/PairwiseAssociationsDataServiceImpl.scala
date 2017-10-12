package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm.{BatchSql, Macro, NamedParameter, SQL}
import uk.ac.ncl.openlab.intake24.errors.{UnexpectedDatabaseError, UpdateError}
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.{PairwiseAssociationRules, PairwiseAssociationRulesConstructorParams}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsDataService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */
class PairwiseAssociationsDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends PairwiseAssociationsDataService with SqlDataService {

  private val coOccurrenceSelectSql = "SELECT * FROM pairwise_associations_co_occurrences;"

  private val occurrencesSelectSql = "SELECT * FROM pairwise_associations_occurrences;"

  private val transactionsCountSelectSql = "SELECT * FROM pairwise_associations_transactions_count;"

  private val coOccurrenceUpdateSql =
    """
      |INSERT INTO pairwise_associations_co_occurrences (locale, antecedent_food_code, consequent_food_code, occurrences)
      |VALUES ({locale}, {antecedent_food_code}, {consequent_food_code}, {occurrences})
      |ON CONFLICT (locale, antecedent_food_code, consequent_food_code)
      |  DO UPDATE SET occurrences = pairwise_associations_co_occurrences.occurrences + {occurrences};
    """.stripMargin

  private val occurrenceUpdateSql =
    """
      |INSERT INTO pairwise_associations_occurrences (locale, food_code, occurrences)
      |VALUES ({locale}, {food_code}, {occurrences})
      |ON CONFLICT (locale, food_code)
      |  DO UPDATE SET occurrences = pairwise_associations_occurrences.occurrences + {occurrences};
    """.stripMargin

  private val transactionCountUpdateSql =
    """
      |INSERT INTO pairwise_associations_transactions_count (locale, transactions_count)
      |VALUES ({locale}, {transactions_count})
      |ON CONFLICT (locale)
      |  DO UPDATE SET transactions_count = pairwise_associations_transactions_count.transactions_count + {transactions_count};
    """.stripMargin

  private case class CoOccurrenceRow(locale: String, antecedent_food_code: String, consequent_food_code: String, occurrences: Int)

  private case class OccurrenceRow(locale: String, food_code: String, occurrences: Int)

  private case class TransactionCountRow(locale: String, transactions_count: Int)

  override def getAssociations(): Either[UnexpectedDatabaseError, Map[String, PairwiseAssociationRules]] = {
    for (
      transactionCounts <- getTransactionCounts();
      occurrences <- getOccurrenceMap();
      coOccurrences <- getCoOccurrenceMap()
    ) yield occurrences.flatMap { occurrenceNode =>
      for (
        transactionCount <- transactionCounts.get(occurrenceNode._1);
        coOccurrenceMap <- coOccurrences.get(occurrenceNode._1);
        paramsNode = occurrenceNode._1 -> {
          val params = PairwiseAssociationRulesConstructorParams(transactionCount, occurrenceNode._2, coOccurrenceMap)
          PairwiseAssociationRules(Some(params))
        }
      ) yield paramsNode
    }
  }

  override def writeAssociations(localeAssociations: Map[String, PairwiseAssociationRules]): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      val occurrenceUpdateParams = localeAssociations.flatMap { localeNode =>
        localeNode._2.getParams().occurrences.map { ocNode =>
          Seq[NamedParameter]('locale -> localeNode._1, 'food_code -> ocNode._1, 'occurrences -> ocNode._2)
        }
      }.toSeq
      val coOccurrenceUpdateParams = localeAssociations.flatMap { localeNode =>
        localeNode._2.getParams().coOccurrences.flatMap { ocNode =>
          ocNode._2.map { consItemNode =>
            Seq[NamedParameter]('locale -> localeNode._1, 'antecedent_food_code -> ocNode._1, 'consequent_food_code -> consItemNode._1, 'occurrences -> consItemNode._2)
          }
        }
      }.toSeq
      val transactionsUpdateParams = localeAssociations.map { localeNode =>
        Seq[NamedParameter]('locale -> localeNode._1, 'transactions_count -> localeNode._2.getParams().numberOfTransactions)
      }.toSeq

      SQL(
        """
          |TRUNCATE pairwise_associations_occurrences;
          |TRUNCATE pairwise_associations_co_occurrences;
          |TRUNCATE pairwise_associations_transactions_count;
        """.stripMargin).execute()

      if (occurrenceUpdateParams.nonEmpty && coOccurrenceUpdateParams.nonEmpty && transactionsUpdateParams.nonEmpty) {
        BatchSql(
          """
            |INSERT INTO pairwise_associations_occurrences (locale, food_code, occurrences)
            |VALUES ({locale}, {food_code}, {occurrences});
          """.stripMargin, occurrenceUpdateParams.head, occurrenceUpdateParams.tail: _*).execute()

        BatchSql(
          """
            |INSERT INTO pairwise_associations_co_occurrences (locale, antecedent_food_code, consequent_food_code, occurrences)
            |VALUES ({locale}, {antecedent_food_code}, {consequent_food_code}, {occurrences});
          """.stripMargin, coOccurrenceUpdateParams.head, coOccurrenceUpdateParams.tail: _*).execute()

        BatchSql(
          """
            |INSERT INTO pairwise_associations_transactions_count (locale, transactions_count)
            |VALUES ({locale}, {transactions_count});
          """.stripMargin, transactionsUpdateParams.head, transactionsUpdateParams.tail: _*).execute()
      }
      Right(())
  }

  override def addTransactions(locale: String, transactions: Seq[Seq[String]]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      val p = PairwiseAssociationRules(None)
      p.addTransactions(transactions)

      val params = p.getParams()
      val coOccurrenceUpdateParams = params.coOccurrences.flatMap { antFoodNode =>
        antFoodNode._2.map { consFoodNode =>
          Seq[NamedParameter]('locale -> locale, 'antecedent_food_code -> antFoodNode._1,
            'consequent_food_code -> consFoodNode._1, 'occurrences -> consFoodNode._2)
        }
      }.toSeq
      val occurrenceUpdateParams = params.occurrences.map { node =>
        Seq[NamedParameter]('locale -> locale, 'food_code -> node._1, 'occurrences -> node._2)
      }.toSeq
      val transactionCountsUpdateParams = Seq[NamedParameter]('locale -> locale, 'transactions_count -> params.numberOfTransactions)

      BatchSql(coOccurrenceUpdateSql, coOccurrenceUpdateParams.head, coOccurrenceUpdateParams.tail: _*).execute()
      BatchSql(occurrenceUpdateSql, occurrenceUpdateParams.head, occurrenceUpdateParams.tail: _*).execute()
      SQL(transactionCountUpdateSql).on(transactionCountsUpdateParams: _*).execute()
      Right(())
  }

  private def getCoOccurrenceMap(): Either[UnexpectedDatabaseError, Map[String, Map[String, Map[String, Int]]]] = tryWithConnection {
    implicit conn =>
      val mp = SQL(coOccurrenceSelectSql).executeQuery().as(Macro.namedParser[CoOccurrenceRow].*).groupBy(_.locale).map { localeNode =>
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
      val mp = SQL(occurrencesSelectSql).executeQuery().as(Macro.namedParser[OccurrenceRow].*).groupBy(_.locale).map { n =>
        n._1 -> n._2.groupBy(_.food_code).map { fn => fn._1 -> fn._2.head.occurrences }
      }
      Right(mp)
  }

  private def getTransactionCounts(): Either[UnexpectedDatabaseError, Map[String, Int]] = tryWithConnection {
    implicit conn =>
      val mp = SQL(transactionsCountSelectSql).executeQuery().as(Macro.namedParser[TransactionCountRow].*).groupBy(_.locale).map { n =>
        n._1 -> n._2.head.transactions_count
      }
      Right(mp)
  }

}
