package uk.ac.ncl.openlab.intake24.foodsql

import javax.sql.DataSource

import anorm.{Macro, SQL, SqlParser, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.errors.{AnyError, NutrientMappingError, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{FoodDataService, ResolvedFoodData}
import uk.ac.ncl.openlab.intake24.services.nutrition.{NutrientDescription, NutrientMappingService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.surveydata.{NutrientMappedFood, NutrientMappedMeal, NutrientMappedSubmission, SurveySubmission}

@Singleton
class NutrientMappingServiceSqlImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource,
                                              foodDataService: FoodDataService,
                                              foodGroupsService: FoodGroupsAdminService) extends NutrientMappingService with SqlDataService with SqlResourceLoader {

  import uk.ac.ncl.openlab.intake24.errors.ErrorUtils._

  private val logger = LoggerFactory.getLogger(classOf[NutrientMappingServiceSqlImpl])

  private case class NutrientDescriptionRow(id: Long, description: String, symbol: String)

  def supportedNutrients(): Either[UnexpectedDatabaseError, Seq[NutrientDescription]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT nutrient_types.id, nutrient_types.description, nutrient_units.symbol FROM nutrient_types INNER JOIN nutrient_units ON nutrient_types.unit_id = nutrient_units.id")
        .executeQuery()
        .as(Macro.namedParser[NutrientDescriptionRow].*)
        .map {
          row => NutrientDescription(row.id, row.description, row.symbol)
        })
  }

  private case class NutrientsRow(nutrient_type_id: Long, units_per_100g: Double)

  def nutrientsFor(table_id: String, record_id: String, weight: Double): Either[NutrientMappingError, Map[Long, Double]] = tryWithConnection {
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

        val result = rows.map(row => (row.nutrient_type_id -> (weight * row.units_per_100g / 100.0))).toMap

        Right(result)
      }
  }

  def energyKcalNutrientId(): Long = 1

  def mapSubmission(submission: SurveySubmission, locale: String): Either[AnyError, NutrientMappedSubmission] = tryWithConnection {
    implicit conn =>

      // I have spent an hour writing an "optimal" solution for this, but then realised that food data inheritance is tricky
      // and it is better to solve performance issues using caching :(

      // Lesson learned: trust your past self.

      val foodCodes = submission.meals.flatMap(_.foods).map(_.code).distinct

      val result =
        for (foodData <- sequence(foodCodes.map(foodDataService.getFoodData(_, locale))).right;
             foodDataMap <- Right(foodData.map(_._1).foldLeft(Map[String, ResolvedFoodData]()) {
               case (map, data) => map + (data.code) -> data
             }).right;
             foodGroupData <- sequence(foodData.map(_._1.groupCode).distinct.map(foodGroupsService.getFoodGroup(_, locale))).right;
             foodGroupMap <- Right(foodGroupData.map(_._1).foldLeft(Map[Int, FoodGroupRecord]()) {
               case (map, record) => map + (record.) -> record
             }).right;)
        ) yield 1
      /*


 .flatMap {
     foodData =>

       val foodDataMap = foodData.map(_._1).foldLeft(Map[String, ResolvedFoodData]()) {
         case (map, data) => map + (data.code) -> data
       }

       val (unmappedCodes, mappedCodes) = foodCodes.partition(foodDataMap(_).nutrientTableCodes.isEmpty)

       logger.warn(s"Some foods are missing nutrient table mapping for locale $locale: ${unmappedCodes.mkString(",")}")

       val (failed, successful) = mappedCodes.map {
         code =>
           val mapping = foodDataMap(code).nutrientTableCodes
           if (mapping.size > 1)
             logger.warn(s"Food $code has more than one nutrient table mapping, the first one will be chosen arbitrarily")

           nutrientsFor(mapping.head._1, mapping.head._2, 100.0).right.map(n => (code, (mapping.head, n))).left.map(e => (code, mapping.head))
       }.partition(_.isLeft)

       val errors = failed.map {
         case (Left((code, (tableId, recordId)))) => s"$tableId/$recordId for food $locale/$code"
       }

       logger.warn(s"Some nutrient table records could not be retrieved: ${errors.mkString(",")}")



       val nutrientDataMap = successful.map(_.right.get).toMap

       val mappedMeals = submission.meals.map {
         meal =>
           val mappedFoods = meal.foods.map {
             food =>
               val foodData = foodDataMap(food.code)
               val nutrientData = nutrientDataMap.get(food.code)

               val tableId = nutrientData.map(_._1._1)
               val recordId = nutrientData.map(_._1._2)
               val nutrientMap = nutrientData.map(_._2).getOrElse(Map())

               val weight = food.portionSize.portionWeight

               NutrientMappedFood(food.code, foodData.englishDescription, foodData.localDescription, food.isReadyMeal, food.searchTerm, food.brand, food.portionSize, food.customData,
                 tableId, recordId, weight <= foodData.reasonableAmount.toDouble, foodData.groupCode, foodData.
               )
           }
       }

   }
}
*/
      translateDatabaseResult(result)
  }

}
