package uk.ac.ncl.openlab.intake24.foodsql

import com.google.inject.Inject
import com.google.inject.name.Named
import com.google.inject.Singleton

import anorm.Macro
import anorm.SQL
import anorm.sqlToSimple
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.nutrients.Nutrient
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientDescription
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService

import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.NutrientMappingError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordType

@Singleton
class NutrientMappingServiceSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends NutrientMappingService with FoodDataSqlService {

  private case class NutrientDescriptionRow(id: Int, description: String, symbol: String)

  def supportedNutrients(): Either[UnexpectedDatabaseError, Seq[NutrientDescription]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT nutrient_types.id, nutrient_types.description, nutrient_units.symbol FROM nutrient_types INNER JOIN nutrient_units ON nutrient_types.unit_id = nutrient_units.id")
        .executeQuery()
        .as(Macro.namedParser[NutrientDescriptionRow].*)
        .map {
          row => NutrientDescription(Nutrient.for_id(row.id).get, row.description, row.symbol)
        })
  }

  private case class NutrientsRow(nutrient_type_id: Int, units_per_100g: Double)

  def nutrientsFor(table_id: String, record_id: String, weight: Double): Either[NutrientMappingError, Map[Nutrient, Double]] = tryWithConnection {
    implicit conn =>
      val rows = SQL("SELECT nutrient_type_id, units_per_100g FROM nutrient_table_records_nutrients WHERE nutrient_table_record_id={record_id} and nutrient_table_id={table_id}")
        .on('record_id -> record_id, 'table_id -> table_id)
        .as(Macro.namedParser[NutrientsRow].*)

      if (rows.isEmpty)
        Left(RecordNotFound(new RuntimeException(s"table_id: $table_id, record_id: $record_id")))
      else
        Right(rows.map(row => (Nutrient.for_id(row.nutrient_type_id).get -> (weight * row.units_per_100g / 100.0))).toMap)
  }
}

