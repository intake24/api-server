package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.sql.DataSource

import anorm.{Macro, SQL, SqlParser, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{FoodCompositionTableError, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.nutrition.{FoodCompositionService, NutrientDescription}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}


@Singleton
class FoodCompositionServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends FoodCompositionService with SqlDataService with SqlResourceLoader {

  private val logger = LoggerFactory.getLogger(classOf[FoodCompositionServiceImpl])

  private case class NutrientDescriptionRow(id: Long, description: String, symbol: String)

  def getSupportedNutrients(): Either[UnexpectedDatabaseError, Seq[NutrientDescription]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT nutrient_types.id, nutrient_types.description, nutrient_units.symbol FROM nutrient_types INNER JOIN nutrient_units ON nutrient_types.unit_id = nutrient_units.id")
        .executeQuery()
        .as(Macro.namedParser[NutrientDescriptionRow].*)
        .map {
          row => NutrientDescription(row.id, row.description, row.symbol)
        })
  }

  private case class NutrientsRow(nutrient_type_id: Long, units_per_100g: Double)

  def getFoodCompositionRecord(table_id: String, record_id: String): Either[FoodCompositionTableError, Map[Long, Double]] = tryWithConnection {
    implicit conn =>
      val validation = SQL("SELECT 1 FROM nutrient_table_records WHERE id={record_id} AND nutrient_table_id={table_id}")
        .on('record_id -> record_id, 'table_id -> table_id)
        .executeQuery().as(SqlParser.long(1).singleOpt).isDefined

      if (!validation)
        Left(RecordNotFound(new RuntimeException(s"table_id: $table_id, record_id: $record_id")))
      else {
        val rows = SQL("SELECT nutrient_type_id, units_per_100g FROM nutrient_table_records_nutrients WHERE nutrient_table_record_id={record_id} and nutrient_table_id={table_id}")
          .on('record_id -> record_id, 'table_id -> table_id)
          .as(Macro.namedParser[NutrientsRow].*)

        val result = rows.map(row => (row.nutrient_type_id -> row.units_per_100g)).toMap

        Right(result)
      }
  }

  def getEnergyKcalNutrientId(): Long = 1
}
