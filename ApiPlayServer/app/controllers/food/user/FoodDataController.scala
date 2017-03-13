package controllers.food.user

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.{AssociatedFood, PortionSizeMethod}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService
import uk.ac.ncl.openlab.intake24.services.fooddb.user._
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService

import scala.concurrent.Future

case class FoodDataForSurvey(code: String, localDescription: String, readyMealOption: Boolean, sameAsBeforeOption: Boolean,
                             caloriesPer100g: Double, portionSizeMethods: Seq[PortionSizeMethod], associatedFoods: Seq[AssociatedFood],
                             brands: Seq[String], categories: Set[String])

class FoodDataController @Inject()(foodDataService: FoodDataService,
                                   foodBrowsingService: FoodBrowsingService,
                                   associatedFoodsService: AssociatedFoodsService,
                                   brandNamesService: BrandNamesService,
                                   asServedImageService: AsServedSetsService,
                                   drinkwareService: DrinkwareService,
                                   guideImageService: GuideImageService,
                                   nutrientMappingService: NutrientMappingService,
                                   imageStorageService: ImageStorageService,
                                   deadbolt: DeadboltActionsAdapter) extends Controller with DatabaseErrorHandler {

  def getCategoryContents(code: String, locale: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(foodBrowsingService.getCategoryContents(code, locale))
      }
  }

  def getFoodData(code: String, locale: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        val energyKcalId = nutrientMappingService.energyKcalNutrientId()

        val result = for (
          foodData <- foodDataService.getFoodData(code, locale).right.map(_._1).right;
          caloriesPer100g <- nutrientMappingService.nutrientsFor(foodData.nutrientTableCodes.head._1, foodData.nutrientTableCodes.head._2, 100).right.map(_ (energyKcalId)).right;
          associatedFoods <- associatedFoodsService.getAssociatedFoods(code, locale).right;
          brands <- brandNamesService.getBrandNames(code, locale).right;
          categories <- foodBrowsingService.getFoodAllCategories(code).right
        ) yield FoodDataForSurvey(foodData.code, foodData.localDescription, foodData.readyMealOption, foodData.sameAsBeforeOption, caloriesPer100g,
          foodData.portionSizeMethods, associatedFoods, brands, categories)

        translateDatabaseResult(result)
      }
  }

  def getFoodDataWithSources(code: String, locale: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(foodDataService.getFoodData(code, locale))
      }
  }

  def getAssociatedFoodPrompts(code: String, locale: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(associatedFoodsService.getAssociatedFoods(code, locale))
      }
  }

  def getBrandNames(code: String, locale: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(brandNamesService.getBrandNames(code, locale))
      }
  }

  def getAsServedSet(id: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(asServedImageService.getAsServedSet(id).right.map {
          set =>
            val images = set.images.map {
              image => UserAsServedImageWithUrls(imageStorageService.getUrl(image.mainImagePath), imageStorageService.getUrl(image.thumbnailPath), image.weight)
            }

            UserAsServedSetWithUrls(imageStorageService.getUrl(set.selectionImagePath), images)
        })
      }
  }

  def getDrinkwareSet(id: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(drinkwareService.getDrinkwareSet(id))
      }
  }

  def getGuideImage(id: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(guideImageService.getGuideImage(id))
      }
  }
}
