package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import java.sql.Connection

import javax.inject.{Inject, Named}
import javax.sql.DataSource
import anorm.{BatchSql, Macro, NamedParameter, SQL, SqlParser}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{UnexpectedDatabaseError, UpdateError}
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.{PairwiseAssociationRules, PairwiseAssociationRulesConstructorParams}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsServiceConfiguration}
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
  * Created by Tim Osadchiy on 02/10/2017.
  */
class PairwiseAssociationsDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource, settings: PairwiseAssociationsServiceConfiguration) extends PairwiseAssociationsDataService with SqlDataService {

  private val THREAD_SLEEP_FOR = 10

  private final val pairwiseAssociationsOccurrencesTN = "pairwise_associations_occurrences"
  private final val pairwiseAssociationsCoOccurrencesTN = "pairwise_associations_co_occurrences"
  private final val pairwiseAssociationsTransactionsCountTN = "pairwise_associations_transactions_count"

  private val logger = LoggerFactory.getLogger(getClass)

  private val coOccurrenceSelectSql = s"SELECT * FROM $pairwiseAssociationsCoOccurrencesTN OFFSET {offset} LIMIT {limit}"

  private val occurrencesSelectSql = s"SELECT * FROM $pairwiseAssociationsOccurrencesTN OFFSET {offset} LIMIT {limit}"

  private val transactionsCountSelectSql = s"SELECT * FROM $pairwiseAssociationsTransactionsCountTN;"

  private val tableCopySuffix = "_copy"

  private val coOccurrenceUpdateSql =
    s"""
       |INSERT INTO $pairwiseAssociationsCoOccurrencesTN (locale, antecedent_food_code, consequent_food_code, occurrences)
       |VALUES ({locale}, {antecedent_food_code}, {consequent_food_code}, {occurrences})
       |ON CONFLICT (locale, antecedent_food_code, consequent_food_code)
       |  DO UPDATE SET occurrences = $pairwiseAssociationsCoOccurrencesTN.occurrences + {occurrences};
    """.stripMargin

  private val occurrenceUpdateSql =
    s"""
       |INSERT INTO $pairwiseAssociationsOccurrencesTN (locale, food_code, occurrences)
       |VALUES ({locale}, {food_code}, {occurrences})
       |ON CONFLICT (locale, food_code)
       |  DO UPDATE SET occurrences = $pairwiseAssociationsOccurrencesTN.occurrences + {occurrences};
    """.stripMargin

  private val transactionCountUpdateSql =
    s"""
       |INSERT INTO $pairwiseAssociationsTransactionsCountTN (locale, transactions_count)
       |VALUES ({locale}, {transactions_count})
       |ON CONFLICT (locale)
       |  DO UPDATE SET transactions_count = $pairwiseAssociationsTransactionsCountTN.transactions_count + {transactions_count};
    """.stripMargin

  private case class CoOccurrenceRow(locale: String, antecedent_food_code: String, consequent_food_code: String, occurrences: Int)

  private case class OccurrenceRow(locale: String, food_code: String, occurrences: Int)

  private case class TransactionCountRow(locale: String, transactions_count: Int)

  override def getAssociations(): Future[Map[String, PairwiseAssociationRules]] = {
    for (
      transactionCounts <- getTransactionCounts();
      occurrences <- getOccurrenceMapAsync();
      coOccurrences <- getCoOccurenceMapAsync()
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

  override def writeAssociations(localeAssociations: Map[String, PairwiseAssociationRules]): Future[Either[UpdateError, Unit]] = {
    val prom = Promise[Either[UpdateError, Unit]]
    val thread = new Thread(() => {
      val r = writeAssociationsToDb(localeAssociations)
      prom.success(r)
    })
    thread.start()
    prom.future
  }

  override def addTransactions(locale: String, transactions: Seq[Seq[String]]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
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
  }

  private def writeAssociationsToDb(localeAssociations: Map[String, PairwiseAssociationRules]): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val tableNames = Seq(pairwiseAssociationsOccurrencesTN, pairwiseAssociationsCoOccurrencesTN, pairwiseAssociationsTransactionsCountTN)

        logger.debug("Copying db tables structure")
        createDbCopies(tableNames, tableCopySuffix)

        localeAssociations.foreach { localeNode =>
          logger.debug(s"Writing graph for locale: ${localeNode._1}")

          val locale = localeNode._1
          val params = localeNode._2.getParams()

          logger.debug("Writing occurrences")
          writeOccurrenceMapToDbInBatch(
            s"""
               |INSERT INTO $pairwiseAssociationsOccurrencesTN$tableCopySuffix (locale, food_code, occurrences)
               |VALUES ({locale}, {food_code}, {occurrences});
            """.stripMargin,
            params.occurrences, kv => Seq[NamedParameter]('locale -> locale, 'food_code -> kv._1, 'occurrences -> kv._2)
          )

          logger.debug("Writing co-occurrences")
          params.coOccurrences.foreach { coocNode =>
            writeOccurrenceMapToDbInBatch(
              s"""
                 |INSERT INTO $pairwiseAssociationsCoOccurrencesTN$tableCopySuffix (locale, antecedent_food_code, consequent_food_code, occurrences)
                 |VALUES ({locale}, {antecedent_food_code}, {consequent_food_code}, {occurrences});
              """.stripMargin,
              coocNode._2,
              consItemNode =>
                Seq[NamedParameter]('locale -> localeNode._1, 'antecedent_food_code -> coocNode._1, 'consequent_food_code -> consItemNode._1, 'occurrences -> consItemNode._2)
            )
          }

          logger.debug("Writing transaction count")
          SQL(
            s"""
               |INSERT INTO $pairwiseAssociationsTransactionsCountTN$tableCopySuffix (locale, transactions_count)
               |VALUES ({locale}, {transactions_count});
            """.stripMargin).on('locale -> localeNode._1, 'transactions_count -> params.numberOfTransactions).execute()
        }

        logger.debug("Committing new tables")
        commitCopies(tableNames, tableCopySuffix)

        Right(())
      }
  }

  private def writeOccurrenceMapToDbInBatch(query: String, mp: Map[String, Int], namedParameterExtractFn: ((String, Int)) => Seq[NamedParameter])(implicit connection: Connection) = {
    Range(0, mp.size, settings.batchSize).foreach { offset =>
      val params = mp.slice(offset, offset + settings.batchSize).map(namedParameterExtractFn).toSeq
      BatchSql(query, params.head, params.tail: _*).execute()
      Thread.sleep(THREAD_SLEEP_FOR)
    }
  }

  private def commitCopies(tableNames: Seq[String], suffix: String)(implicit connection: Connection) = {
    val q = (tn: String) =>
      s"""
         |DROP TABLE $tn;
         |ALTER TABLE $tn$suffix RENAME TO $tn;
      """.stripMargin
    val qs = tableNames.map(q).mkString
    SQL(qs).execute()
  }

  private def createDbCopies(tableNames: Seq[String], suffix: String)(implicit conn: Connection) = {
    val q = (tn: String) => s"CREATE TABLE $tn$suffix (LIKE $tn INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);"
    val qs = tableNames.map(q).mkString
    SQL(qs).execute()
  }

  private def getCoOccurenceMapAsync(): Future[Map[String, Map[String, Map[String, Int]]]] = {
    val promise = Promise[Map[String, Map[String, Map[String, Int]]]]()
    val defaultMap: Map[String, Map[String, Map[String, Int]]] = Map().withDefaultValue(Map().withDefaultValue(Map().withDefaultValue(0)))
    val t = new Thread(() => {
      countRecordsInTable(pairwiseAssociationsCoOccurrencesTN).flatMap { count =>
        logger.info(s"Getting co-occurrences for $count records from DB.")
        val rowsResults = Range(0, count, settings.batchSize).map { offset =>
          Thread.sleep(THREAD_SLEEP_FOR)
          tryWithConnection {
            implicit conn =>
              Right(SQL(coOccurrenceSelectSql).on('limit -> settings.batchSize, 'offset -> offset).executeQuery().as(Macro.namedParser[CoOccurrenceRow].*))
          }
        }.toSeq

        rowsResults.partition(_.isLeft) match {
          case (Nil, results) =>
            logger.info(s"Successfully finished getting co-occurrences for $count records from DB.")
            logger.info(s"Converting co-occurrences into a map")
            Right(results.foldLeft(defaultMap) { (agg, result) => {
              Thread.sleep(THREAD_SLEEP_FOR)
              result.right.get.foldLeft(agg) { (subAgg, record) =>
                subAgg + (record.locale ->
                  (subAgg(record.locale) + (record.antecedent_food_code ->
                    (subAgg(record.locale)(record.antecedent_food_code) + (record.consequent_food_code -> record.occurrences)))))
              }
            }
            })
          case _ =>
            val error = rowsResults.collectFirst({
              case Left(e) => e.exception.getMessage
            }).getOrElse("Unknown error")
            logger.info(s"At least one occurrence batch was retrieved with the following error. $error")
            Left(UnexpectedDatabaseError(new Exception("Couldn't retrieve co-occurrence records")))
        }

      } match {
        case Left(e) => promise.failure(e.exception)
        case Right(mp) => promise.success(mp)
      }
    })
    t.run()
    promise.future
  }

  private def getOccurrenceMapAsync(): Future[Map[String, Map[String, Int]]] = {
    val promise = Promise[Map[String, Map[String, Int]]]
    val defaultMap = Map[String, Map[String, Int]]().withDefaultValue(Map().withDefaultValue(0))
    val t = new Thread(() => {
      countRecordsInTable(pairwiseAssociationsOccurrencesTN).flatMap { count =>
        logger.info(s"Getting occurrences for $count records from DB.")
        val queryResults = Range(0, count, settings.batchSize).map { offset =>
          Thread.sleep(THREAD_SLEEP_FOR)
          tryWithConnection { implicit conn =>
            Right(SQL(occurrencesSelectSql).on('limit -> settings.batchSize, 'offset -> offset)
              .executeQuery()
              .as(Macro.namedParser[OccurrenceRow].*))
          }
        }.toSeq
        queryResults.partition(_.isLeft) match {
          case (Nil, results) =>
            logger.info(s"Successfully finished getting occurrences for $count records from DB.")
            logger.info(s"Converting occurrences into a map")
            Right(results.foldLeft(defaultMap) { (agg, result) =>
              result.right.get.foldLeft(agg) { (agg, occ) =>
                agg + (occ.locale -> (agg(occ.locale) + (occ.food_code -> occ.occurrences)))
              }
            })
          case _ =>
            val error = queryResults.collectFirst({
              case Left(e) => e.exception.getMessage
            }).getOrElse("Unknown error")
            logger.info(s"At least one occurrence batch was retrieved with the following error. $error")
            Left(UnexpectedDatabaseError(new Exception("Couldn't retieve occurence records")))
        }
      } match {
        case Left(e) => promise.failure(e.exception)
        case Right(mp) => promise.success(mp)
      }
    })
    t.run()
    promise.future
  }

  private def getTransactionCounts(): Future[Map[String, Int]] = {
    val promise = Promise[Map[String, Int]]()
    val t = new Thread(() => {
      tryWithConnection {
        implicit conn =>
          val mp = SQL(transactionsCountSelectSql).executeQuery().as(Macro.namedParser[TransactionCountRow].*).groupBy(_.locale).map { n =>
            n._1 -> n._2.head.transactions_count
          }
          Right(mp)
      } match {
        case Right(mp) => promise.success(mp)
        case Left(e) => promise.failure(e.exception)
      }
    })
    t.run()
    promise.future
  }

  private def countRecordsInTable(tableName: String): Either[UnexpectedDatabaseError, Int] = tryWithConnection {
    implicit conn => Right(SQL(s"SELECT count(*) FROM $tableName").executeQuery().as(SqlParser.int("count").single))
  }

}
