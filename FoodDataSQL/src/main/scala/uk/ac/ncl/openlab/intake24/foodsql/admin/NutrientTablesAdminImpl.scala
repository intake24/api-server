package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import anorm.{AnormUtil, BatchSql, Macro, NamedParameter, SQL, SqlParser, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.apache.commons.lang3.StringUtils
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{NutrientTablesAdminService, SingleNutrientTypeUpdate}
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import uk.ac.ncl.openlab.intake24.{NewNutrientTableRecord, NutrientTable, NutrientTableRecord}

@Singleton
class NutrientTablesAdminImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends NutrientTablesAdminService with SqlDataService {

  private case class NutrientTableDescRow(id: String, description: String) {
    def asNutrientTable = NutrientTable(id, description)
  }

  private case class NutrientTableRecordRow(id: String, nutrient_table_id: String, english_description: String, local_description: Option[String]) {
    def toNutrientTableRecord = NutrientTableRecord(id, nutrient_table_id, english_description, local_description)
  }

  def listNutrientTables(): Either[UnexpectedDatabaseError, Map[String, NutrientTable]] = tryWithConnection {
    implicit conn =>
      val query = "SELECT id, description FROM nutrient_tables"
      Right(SQL(query).executeQuery().as(Macro.namedParser[NutrientTableDescRow].*).foldLeft(Map[String, NutrientTable]()) {
        (result, row) => result + (row.id -> row.asNutrientTable)
      })
  }

  def searchNutrientTableRecords(nutrientTableId: String, query: Option[String]): Either[UnexpectedDatabaseError, Seq[NutrientTableRecord]] = tryWithConnection {
    implicit conn =>
      val sqlQuery =
        """
          |SELECT id, nutrient_table_id, english_description, local_description
          |FROM nutrient_table_records
          |WHERE nutrient_table_id = {nutrient_table_id} AND
          |(id ILIKE {query} OR english_description ILIKE {query} OR local_description ILIKE {query})
          |ORDER BY id LIMIT {limit};
        """.stripMargin
      val result = SQL(sqlQuery).on(
        'nutrient_table_id -> nutrientTableId,
        'query -> s"%${AnormUtil.escapeLike(StringUtils.stripAccents(query.getOrElse("")))}%",
        'limit -> 20
      ).executeQuery().as(Macro.namedParser[NutrientTableRecordRow].*).map(_.toNutrientTableRecord)
      Right(result)
  }

  def getNutrientTable(id: String): Either[LookupError, NutrientTable] = tryWithConnection {
    implicit conn =>
      var query = """SELECT id, description FROM nutrient_tables WHERE id = {id} ORDER BY english_name"""

      SQL(query).on('id -> id).executeQuery().as(Macro.namedParser[NutrientTable].singleOpt) match {
        case Some(table) => Right(table)
        case None => Left(RecordNotFound(new RuntimeException(id)))
      }
  }

  def createNutrientTable(data: NutrientTable): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO nutrient_tables VALUES({id}, {description})"""

      SQL(query).on('id -> data.id, 'description -> data.description).execute()

      Right(())
  }

  def createOrUpdateNutrientTable(data: NutrientTable): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO nutrient_tables VALUES({id}, {description}) ON CONFLICT ON CONSTRAINT nutrient_tables_pk DO UPDATE SET description=EXCLUDED.description"""

      SQL(query).on('id -> data.id, 'description -> data.description).execute()

      Right(())
  }

  def updateNutrientTable(id: String, data: NutrientTable): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """UPDATE nutrient_tables SET id={new_id}, description={description} WHERE id = {id}"""

      val affectedRows = SQL(query).on('id -> id, 'new_id -> data.id, 'description -> data.description).executeUpdate()

      if (affectedRows == 0)
        Left(RecordNotFound(new RuntimeException(id)))
      else
        Right(())
  }

  def deleteNutrientTable(id: String): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      val query = """DELETE FROM nutrient_tables WHERE id={id}"""

      val affectedRows = SQL(query).on('id -> id).executeUpdate()

      if (affectedRows == 0)
        Left(RecordNotFound(new RuntimeException(id)))
      else
        Right(())
  }

  def deleteAllNutrientTables(): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM nutrient_tables").execute()
      Right(())
  }

  private val nutrientsInsertQuery = "INSERT INTO nutrient_table_records_nutrients VALUES({record_id},{nutrient_table_id},{nutrient_type_id},{units_per_100g})"

  def createNutrientTableRecords(records: Seq[NewNutrientTableRecord]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      withTransaction {
        val recordQuery = """INSERT INTO nutrient_table_records VALUES({id},{nutrient_table_id},{english_description},{local_description})"""

        val recordParams =
          records.map(r => Seq[NamedParameter]('id -> r.id, 'nutrient_table_id -> r.nutrientTableId, 'english_description -> r.description, 'local_description -> r.localDescription))

        val nutrientParams =
          records.flatMap {
            record =>
              record.nutrients.map {
                case (nutrientType, unitsPer100g) =>
                  Seq[NamedParameter]('record_id -> record.id, 'nutrient_table_id -> record.nutrientTableId, 'nutrient_type_id -> nutrientType, 'units_per_100g -> unitsPer100g)
              }
          }

        batchSql(recordQuery, recordParams).execute()
        batchSql(nutrientsInsertQuery, nutrientParams).execute()

        Right(())
      }
  }

  def createOrUpdateNutrientTableRecords(records: Seq[NewNutrientTableRecord]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      if (records.nonEmpty) {

        withTransaction {
          val recordUpsertQuery =
            """INSERT INTO nutrient_table_records VALUES({id},{nutrient_table_id},{english_description},{local_description})
              |ON CONFLICT ON CONSTRAINT nutrient_table_records_pk DO UPDATE SET english_description=EXCLUDED.english_description, local_description=EXCLUDED.local_description""".stripMargin

          val recordParams =
            records.map(r => Seq[NamedParameter]('id -> r.id, 'nutrient_table_id -> r.nutrientTableId, 'english_description -> r.description, 'local_description -> r.localDescription))

          BatchSql(recordUpsertQuery, recordParams.head, recordParams.tail: _*).execute()

          val nutrientDeleteParams = records.map {
            record =>
              Seq[NamedParameter]('table_id -> record.nutrientTableId, 'record_id -> record.id)
          }

          BatchSql("DELETE FROM nutrient_table_records_nutrients WHERE nutrient_table_id={table_id} AND nutrient_table_record_id={record_id}", nutrientDeleteParams.head, nutrientDeleteParams.tail: _*).execute()

          val nutrientUpdateParams =
            records.flatMap {
              record =>
                record.nutrients.map {
                  case (nutrientType, unitsPer100g) =>
                    Seq[NamedParameter]('record_id -> record.id, 'nutrient_table_id -> record.nutrientTableId, 'nutrient_type_id -> nutrientType, 'units_per_100g -> unitsPer100g)
                }
            }


          BatchSql(nutrientsInsertQuery, nutrientUpdateParams.head, nutrientUpdateParams.tail: _*).execute()

          Right(())
        }
      } else Right(())
  }


  def updateSingleNutrientType(nutrientTableId: String, nutrientTypeId: Long, updates: Seq[SingleNutrientTypeUpdate]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      withTransaction {

        val (update, delete) = updates.partition(_.newValue.isDefined)

        val deleteParams = delete.map {
          record =>
            Seq[NamedParameter]('table_id -> nutrientTableId, 'nutrient_type_id -> nutrientTypeId, 'record_id -> record.nutrientTableRecordId)
        }

        if (deleteParams.nonEmpty) {
          val deleteQuery = "DELETE FROM nutrient_table_records_nutrients WHERE nutrient_table_id={table_id} AND nutrient_table_record_id={record_id} AND nutrient_type_id={nutrient_type_id}"

          BatchSql(deleteQuery, deleteParams.head, deleteParams.tail: _*).execute()
        }

        val updateParams = update.map {
          record =>
            Seq[NamedParameter]('table_id -> nutrientTableId, 'nutrient_type_id -> nutrientTypeId, 'record_id -> record.nutrientTableRecordId, 'units_per_100g -> record.newValue.get)
        }

        if (updateParams.nonEmpty) {

          val nutrientUpsertQuery =
            """INSERT INTO nutrient_table_records_nutrients VALUES({record_id}, {table_id}, {nutrient_type_id}, {units_per_100g})
              |ON CONFLICT ON CONSTRAINT nutrient_table_records_nutrients_pk DO UPDATE SET units_per_100g=EXCLUDED.units_per_100g""".stripMargin

          BatchSql(nutrientUpsertQuery, updateParams.head, updateParams.tail: _*).execute()
        }

        Right(())
      }
  }

  def updateNutrientTableRecords(records: Seq[NewNutrientTableRecord]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        if (records.nonEmpty) {
          val deleteParams = records.map {
            record =>
              Seq[NamedParameter]('table_id -> record.nutrientTableId, 'record_id -> record.id)
          }

          BatchSql("DELETE FROM nutrient_table_records_nutrients WHERE nutrient_table_id={table_id} AND nutrient_table_record_id={record_id}", deleteParams.head, deleteParams.tail: _*).execute()

          val insertParams = records.flatMap {
            record =>
              record.nutrients.map {
                case (nutrientType, unitsPer100g) =>
                  Seq[NamedParameter]('record_id -> record.id, 'nutrient_table_id -> record.nutrientTableId, 'nutrient_type_id -> nutrientType, 'units_per_100g -> unitsPer100g)
              }
          }

          BatchSql(nutrientsInsertQuery, insertParams.head, insertParams.tail: _*).execute()

        }

        Right(())
      }
  }

  def updateNutrientTableRecordDescriptions(nutrients: Seq[NutrientTableRecord]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      val namedParameters = nutrients.map(n =>
        Seq[NamedParameter]('id -> n.id, 'nutrient_table_id -> n.nutrientTableId,
          'english_description -> n.description, 'local_description -> n.localDescription))

      val sqlQuery =
        """
          |UPDATE nutrient_table_records
          |SET english_description = {english_description}, local_description = {local_description}
          |WHERE id = {id} AND nutrient_table_id = {nutrient_table_id};
        """.stripMargin

      BatchSql(sqlQuery, namedParameters.head, namedParameters.tail: _*).execute()

      Right(())
  }

  def getNutrientTableRecordIds(nutrientTableId: String): Either[UnexpectedDatabaseError, Seq[String]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT id FROM nutrient_table_records WHERE nutrient_table_id={nutrient_table_id}")
        .on('nutrient_table_id -> nutrientTableId)
        .as(SqlParser.str("id").*))
  }
}
