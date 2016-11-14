package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.NutrientTable
import anorm.SQL
import anorm.Macro
import anorm.sqlToSimple

import uk.ac.ncl.openlab.intake24.services.fooddb.admin.NutrientTablesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import anorm.NamedParameter
import com.google.inject.Inject
import com.google.inject.Singleton
import javax.sql.DataSource
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordType
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService

@Singleton
class NutrientTablesAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends NutrientTablesAdminImpl

trait NutrientTablesAdminImpl extends NutrientTablesAdminService with FoodDataSqlService {
  private case class NutrientTableDescRow(id: String, description: String) {
    def asNutrientTable = NutrientTable(id, description)
  }

  def listNutrientTables(): Either[UnexpectedDatabaseError, Map[String, NutrientTable]] = tryWithConnection {
    implicit conn =>
      val query = "SELECT id, description FROM nutrient_tables"
      Right(SQL(query).executeQuery().as(Macro.namedParser[NutrientTableDescRow].*).foldLeft(Map[String, NutrientTable]()) {
        (result, row) => result + (row.id -> row.asNutrientTable)
      })
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

  def createNutrientTableRecords(records: Seq[NutrientTableRecord]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      conn.setAutoCommit(false)

      val recordQuery = """INSERT INTO nutrient_table_records VALUES({id},{nutrient_table_id})"""

      val recordParams =
        records.map(r => Seq[NamedParameter]('id -> r.record_id, 'nutrient_table_id -> r.table_id))

      val nutrientsQuery = """INSERT INTO nutrient_table_records_nutrients VALUES({record_id},{nutrient_table_id},{nutrient_type_id},{units_per_100g})"""

      val nutrientParams =
        records.flatMap {
          record =>
            record.nutrients.map {
              case (nutrientType, unitsPer100g) =>
                Seq[NamedParameter]('record_id -> record.record_id, 'nutrient_table_id -> record.table_id, 'nutrient_type_id -> nutrientType.id, 'units_per_100g -> unitsPer100g)
            }
        }

      batchSql(recordQuery, recordParams).execute()
      batchSql(nutrientsQuery, nutrientParams).execute()

      conn.commit()

      Right(())
  }
}
