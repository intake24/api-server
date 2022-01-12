package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import anorm.{BatchSql, Macro, NamedParameter, SQL, SqlParser}
import cats.data.EitherT
import cats.implicits._
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{UnexpectedDatabaseError, UpdateError}
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.{PairwiseAssociationRules, PairwiseAssociationRulesConstructorParams}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsServiceConfiguration}
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

import java.sql.Connection
import java.time.{Instant, ZoneId, ZonedDateTime}
import javax.inject.{Inject, Named}
import javax.sql.DataSource
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Tim Osadchiy on 02/10/2017.
 */
class PairwiseAssociationsDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource,
                                                    settings: PairwiseAssociationsServiceConfiguration,
                                                    @Named("intake24") implicit val executionContext: ExecutionContext) extends PairwiseAssociationsDataService with SqlDataService {

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

  override def getAssociations(): Future[Either[UnexpectedDatabaseError, Map[String, PairwiseAssociationRules]]] = {
    val eitherT = for (
      transactionCounts <- EitherT(getTransactionCounts());
      occurrences <- EitherT(getOccurrenceMapAsync());
      coOccurrences <- EitherT(getCoOccurenceMapAsync())
    ) yield occurrences.flatMap { occurrenceNode =>
      for (
        transactionCount <- transactionCounts.get(occurrenceNode._1);
        coOccurrenceMap <- coOccurrences.get(occurrenceNode._1);
        paramsNode = occurrenceNode._1 -> {
          val params = PairwiseAssociationRulesConstructorParams(transactionCount, occurrenceNode._2, coOccurrenceMap)
          logger.info(s"Successfully retreived graph from DB.")
          PairwiseAssociationRules(Some(params))
        }
      ) yield paramsNode
    }

    eitherT.value
  }

  override def writeAssociations(localeAssociations: Map[String, PairwiseAssociationRules]): Future[Either[UpdateError, Unit]] = {
    Future {
      writeAssociationsToDb(reduceAssociations(localeAssociations))
    }
  }

  override def addTransactions(locale: String, transactions: Seq[Seq[String]]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val p = PairwiseAssociationRules(None)
        p.addTransactions(transactions)

        val params = p.getParams()
        val coOccurrenceUpdateParams = reduceCoOccurrences(params.coOccurrences).map { cooc =>
          Seq[NamedParameter]('locale -> locale, 'antecedent_food_code -> cooc._1,
            'consequent_food_code -> cooc._2, 'occurrences -> cooc._3)
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
          writeToDbInBatch(
            s"""
               |INSERT INTO $pairwiseAssociationsOccurrencesTN$tableCopySuffix (locale, food_code, occurrences)
               |VALUES ({locale}, {food_code}, {occurrences});
            """.stripMargin,
            params.occurrences.map {
              kv => Seq[NamedParameter]('locale -> locale, 'food_code -> kv._1, 'occurrences -> kv._2)
            }.toSeq
          )

          logger.debug("Writing co-occurrences")
          // We write each co-occurrence only once, e.g. a -> (b -> 3) and b -> (a -> 3) becomes (a, b, 3)
          val reduced = reduceCoOccurrences(params.coOccurrences)
          writeToDbInBatch(
            s"""
               |INSERT INTO $pairwiseAssociationsCoOccurrencesTN$tableCopySuffix (locale, antecedent_food_code, consequent_food_code, occurrences)
               |VALUES ({locale}, {antecedent_food_code}, {consequent_food_code}, {occurrences});
              """.stripMargin,
            reduced.map { cooc =>
              Seq[NamedParameter]('locale -> localeNode._1, 'antecedent_food_code -> cooc._1, 'consequent_food_code -> cooc._2, 'occurrences -> cooc._3)
            }.toSeq
          )

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

  private def writeToDbInBatch(query: String, data: Seq[Seq[NamedParameter]])(implicit connection: Connection) = {
    Range(0, data.size, settings.readWriteRulesDbBatchSize).foreach { offset =>
      val params = data.slice(offset, offset + settings.readWriteRulesDbBatchSize)
      BatchSql(query, params.head, params.tail: _*).execute()
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

  private def getCoOccurenceMapAsync(): Future[Either[UnexpectedDatabaseError, Map[String, Map[String, Map[String, Int]]]]] = {
    val defaultMap: Map[String, Map[String, Map[String, Int]]] = Map().withDefaultValue(Map().withDefaultValue(Map().withDefaultValue(0)))
    Future {
      countRecordsInTable(pairwiseAssociationsCoOccurrencesTN).flatMap { count =>
        logger.info(s"Getting co-occurrences for $count records from DB.")
        val rowsResults = Range(0, count, settings.readWriteRulesDbBatchSize).map { offset =>
          tryWithConnection {
            implicit conn =>
              Right(SQL(coOccurrenceSelectSql).on('limit -> settings.readWriteRulesDbBatchSize, 'offset -> offset).executeQuery().as(Macro.namedParser[CoOccurrenceRow].*))
          }
        }.toSeq

        rowsResults.partition(_.isLeft) match {
          case (Nil, results) =>
            logger.info(s"Successfully finished getting co-occurrences for $count records from DB.")
            logger.info(s"Converting co-occurrences into a map")
            Right(results.foldLeft(defaultMap) { (agg, result) => {
              // Co-occurrences are stored as (a, b, 3). We unwrap them to a -> (b -> 3) and b -> (a -> 3)
              result.right.get.foldLeft(agg) { (subAgg, record) =>
                subAgg + (record.locale ->
                  (subAgg(record.locale) +
                    (record.antecedent_food_code ->
                      (subAgg(record.locale)(record.antecedent_food_code) + (record.consequent_food_code -> record.occurrences))) +
                    (record.consequent_food_code ->
                      (subAgg(record.locale)(record.consequent_food_code) + (record.antecedent_food_code -> record.occurrences)))
                    ))
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

      }
    }
  }

  private def getOccurrenceMapAsync(): Future[Either[UnexpectedDatabaseError, Map[String, Map[String, Int]]]] = {
    val defaultMap = Map[String, Map[String, Int]]().withDefaultValue(Map().withDefaultValue(0))
    Future {
      countRecordsInTable(pairwiseAssociationsOccurrencesTN).flatMap { count =>
        logger.info(s"Getting occurrences for $count records from DB.")
        val queryResults = Range(0, count, settings.readWriteRulesDbBatchSize).map { offset =>
          tryWithConnection { implicit conn =>
            Right(SQL(occurrencesSelectSql).on('limit -> settings.readWriteRulesDbBatchSize, 'offset -> offset)
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
      }
    }
  }

  private def getTransactionCounts(): Future[Either[UnexpectedDatabaseError, Map[String, Int]]] =
    Future {
      tryWithConnection {
        implicit conn =>
          val mp = SQL(transactionsCountSelectSql).executeQuery().as(Macro.namedParser[TransactionCountRow].*).groupBy(_.locale).map { n =>
            n._1 -> n._2.head.transactions_count
          }
          Right(mp)
      }
    }

  private def countRecordsInTable(tableName: String): Either[UnexpectedDatabaseError, Int] = tryWithConnection {
    implicit conn => Right(SQL(s"SELECT count(*) FROM $tableName").executeQuery().as(SqlParser.int("count").single))
  }

  private def reduceAssociations(localeAssociations: Map[String, PairwiseAssociationRules]): Map[String, PairwiseAssociationRules] = {
    localeAssociations.map { kv =>
      kv._1 -> {
        val ruleParams = kv._2.getParams()
        kv._2.reduce(settings.storedCoOccurrencesThreshold)
      }
    }
  }

  private def reduceCoOccurrences(coOccurrences: Map[String, Map[String, Int]]): Set[(String, String, Int)] = {
    coOccurrences.foldLeft(Set[(String, String, Int)]()) { (agg, kv) =>
      agg ++ kv._2.foldLeft(agg) { (agg, skv) =>
        val sorted = Vector(kv._1, skv._1).sorted
        agg + ((sorted.head, sorted.last, skv._2))
      }
    }
  }

  override def getLastSubmissionTime(): Either[UnexpectedDatabaseError, ZonedDateTime] = tryWithConnection {
    implicit conn =>
      val time = SQL(s"select last_submission_time from pairwise_associations_state limit 1").executeQuery().as(SqlParser.scalar[ZonedDateTime].singleOpt)
      Right(time.getOrElse(ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("Z"))))
  }

  override def updateLastSubmissionTime(time: ZonedDateTime): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL(s"insert into pairwise_associations_state(id, last_submission_time) values (1, {time}) " +
        s"on conflict(id) do update set last_submission_time={time}").on('time -> time).executeUpdate()
      Right(())
  }
}
