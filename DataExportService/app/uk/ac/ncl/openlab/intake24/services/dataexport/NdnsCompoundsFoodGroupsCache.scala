package uk.ac.ncl.openlab.intake24.services.dataexport

import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}

import org.slf4j.LoggerFactory
import play.api.cache.SyncCacheApi
import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.NdnsCompoundFoodGroupsService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{ExportFood, ExportMeal, ExportSubmission}
import uk.ac.ncl.openlab.intake24.surveydata.{MealTime, MissingFood, PortionSizeWithWeights}


case class ExportSubmissionWithFoodGroups(id: UUID, userId: Int, userAlias: Option[String], userCustomData: Map[String, String], surveyCustomData: Map[String, String], startTime: ZonedDateTime, endTime: ZonedDateTime, meals: Seq[ExportMealWithFoodGroups])

case class ExportMealWithFoodGroups(name: String, time: MealTime, customData: Map[String, String], foods: Seq[ExportFoodWithFoodGroups], missingFoods: Seq[MissingFood])

case class ExportFoodWithFoodGroups(code: String, englishDescription: String, localDescription: Option[String], searchTerm: String, nutrientTableId: String, nutrientTableCode: String, isReadyMeal: Boolean,
                                    portionSize: PortionSizeWithWeights, reasonableAmount: Boolean, foodGroupId: Int, brand: String, nutrients: Map[Int, Double], compoundFoodGroups: Map[Int, Double], customData: Map[String, String])

@Singleton
class NdnsCompoundsFoodGroupsCache @Inject()(service: NdnsCompoundFoodGroupsService,
                                             cache: SyncCacheApi) {

  private val logger = LoggerFactory.getLogger(classOf[NdnsCompoundsFoodGroupsCache])

  private def cacheKey(ndnsCode: Int) = s"ndnsCompoundFoodGroups.$ndnsCode"

  private def getFoodGroupsData(forFoods: Set[Int]): Either[UnexpectedDatabaseError, Map[Int, Map[Int, Double]]] = {
    val z = (Map[Int, Map[Int, Double]](), Set[Int]())

    val (cached, uncached) = forFoods.foldLeft(z) {
      case ((cached, uncached), code) =>
        cache.get[Map[Int, Double]](cacheKey(code)) match {
          case Some(data) => (cached + (code -> data), uncached)
          case None => (cached, uncached + code)
        }
    }

    if (uncached.isEmpty)
      Right(cached)
    else
      service.getCompoundFoodGroupsData(uncached).map {
        dbData =>

          val unavailable = uncached.foldLeft(Map[Int, Map[Int, Double]]()) {
            (acc, uncachedKey) =>
              dbData.get(uncachedKey) match {
                case Some(data) =>
                  cache.set(cacheKey(uncachedKey), data)
                  acc
                case None => {
                  logger.warn(s"No compound food group data available for NDNS code $uncachedKey")
                  cache.set(cacheKey(uncachedKey), Map())
                  acc + (uncachedKey -> Map())
                }
              }
          }

          cached ++ dbData ++ unavailable
      }
  }

  private def collectCodes(submissions: Seq[ExportSubmission]): Set[Int] =
    submissions.flatMap(_.meals.flatMap(_.foods.map(f => (f.nutrientTableId, f.nutrientTableCode))))
      .filter(_._1 == "NDNS")
      .map(_._2.toInt)
      .toSet

  private def mapFood(food: ExportFood, foodGroupData: Map[Int, Map[Int, Double]]): ExportFoodWithFoodGroups = {

    val foodGroups = if (food.nutrientTableId == "NDNS")
      foodGroupData(food.nutrientTableCode.toInt)
    else
      Map[Int, Double]()

    ExportFoodWithFoodGroups(food.code, food.englishDescription, food.localDescription, food.searchTerm, food.nutrientTableId,
      food.nutrientTableCode, food.isReadyMeal, food.portionSize, food.reasonableAmount, food.foodGroupId, food.brand,
      food.nutrients, foodGroups, food.customData)
  }

  private def mapMeal(meal: ExportMeal, foodGroupData: Map[Int, Map[Int, Double]]): ExportMealWithFoodGroups =
    ExportMealWithFoodGroups(meal.name, meal.time, meal.customData, meal.foods.map(f => mapFood(f, foodGroupData)), meal.missingFoods)

  private def mapSubmission(s: ExportSubmission, foodGroupData: Map[Int, Map[Int, Double]]): ExportSubmissionWithFoodGroups =
    ExportSubmissionWithFoodGroups(s.id, s.userId, s.userAlias, s.userCustomData, s.surveyCustomData, s.startTime, s.endTime,
      s.meals.map(m => mapMeal(m, foodGroupData)))

  def addFoodGroups(submissions: Seq[ExportSubmission]): Either[UnexpectedDatabaseError, Seq[ExportSubmissionWithFoodGroups]] =
    getFoodGroupsData(collectCodes(submissions)).map {
      fgData =>
        submissions.map(s => mapSubmission(s, fgData))
    }

}
