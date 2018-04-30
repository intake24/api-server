package uk.ac.ncl.openlab.intake24.foodSubstRec

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.UserFoodHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService
import uk.ac.ncl.openlab.intake24.services.nutrition.{FoodCompositionService, NutrientDescription}

/**
  * Created by Tim Osadchiy on 26/04/2018.
  */
trait FoodSubstApi {

  def findAlternatives(foodCode: String): Seq[FoodWithNutrients]

  def listNutrients(): Seq[NutrientDescription]

  def defaultNutrientIds(): Seq[Long]

}

case class FoodWithNutrients(foodHeader: UserFoodHeader, nutrients: Map[Long, Double])

@Singleton()
class FoodSubstApiImpl @Inject()(foodBrowsingService: FoodBrowsingService,
                                 foodSubstRecommender: FoodSubstRecommender,
                                 nutrientService: FoodNutrientService,
                                 foodCompositionService: FoodCompositionService) extends FoodSubstApi {

  private val logger = LoggerFactory.getLogger(getClass)

  private val ENERGY_ID = 1
  private val FAT_ID = 49
  private val SAT_FAT_ID = 50
  private val SUGARS_ID = 23
  private val CO2 = 228

  private val supportedNutrients = foodCompositionService.getSupportedNutrients() match {
    case Right(r) => r
    case Left(e) =>
      logger.error(s"Couldn't get supported nutrients. ${e.exception.getMessage}")
      Seq()
  }


  // Fixme: should be for any locale
  private val foodHeaders: Map[String, UserFoodHeader] = foodBrowsingService.listAllFoods("en_GB") match {
    case Right(r) => r.groupBy(_.code).map(i => i._1 -> i._2.head)
    case Left(e) =>
      logger.error(s"Couldn't get food headers. ${e.exception.getMessage}")
      Map()
  }

  override def findAlternatives(foodCode: String): Seq[FoodWithNutrients] =
    foodSubstRecommender.findAlternatives(foodCode)
      .flatMap { fc =>
        for (
          foodHeader <- {
            val fh = foodHeaders.get(fc)
            foodHeaders.get(fc)
          };
          foodNutrients <- {
            val nuts = nutrientService.getNutrients(fc)
            nutrientService.getNutrients(fc)
          };
          item = FoodWithNutrients(foodHeader, foodNutrients)
        ) yield item
      }

  override def listNutrients(): Seq[NutrientDescription] = supportedNutrients

  override def defaultNutrientIds(): Seq[Long] = Seq(ENERGY_ID, FAT_ID, SAT_FAT_ID, SUGARS_ID, CO2)

}
