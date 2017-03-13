package controllers.food.user

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.UpickleUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService
import uk.ac.ncl.openlab.intake24.services.fooddb.user._
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService

import scala.concurrent.Future

case class PortionSizeMethodForSurvey(method: String, description: String, imageUrl: String, useForRecipes: Boolean, parameters: Map[String, String])

case class FoodDataForSurvey(code: String, localDescription: String, readyMealOption: Boolean, sameAsBeforeOption: Boolean,
                             caloriesPer100g: Double, portionSizeMethods: Seq[PortionSizeMethodForSurvey], associatedFoods: Seq[AssociatedFood],
                             brands: Seq[String], categories: Set[String])

case class UserAsServedImageWithUrls(mainImageUrl: String, thumbnailUrl: String, weight: Double)

case class UserAsServedSetWithUrls(selectionImageUrl: String, images: Seq[UserAsServedImageWithUrls])


case class UserImageMapObjectWithUrls(id: Int, description: String, overlayUrl: String, outline: Array[Double])

case class UserImageMapWithUrls(baseImageUrl: String, objects: Seq[UserImageMapObjectWithUrls])

case class UserGuideImageWithUrls(description: String, imageMap: UserImageMapWithUrls, weights: Map[String, Double])

case class UserDrinkScaleWithUrls(objectId: Int, baseImageUrl: String, overlayImageUrl: String, width: Int, height: Int, emptyLevel: Int, fullLevel: Int, volumeSamples: Seq[VolumeSample])

case class UserDrinkwareSetWithUrls(guideId: String, scales: Seq[UserDrinkScaleWithUrls])

class FoodDataController @Inject()(foodDataService: FoodDataService,
                                   foodBrowsingService: FoodBrowsingService,
                                   associatedFoodsService: AssociatedFoodsService,
                                   brandNamesService: BrandNamesService,
                                   asServedImageService: AsServedSetsService,
                                   drinkwareService: DrinkwareService,
                                   guideImageService: GuideImageService,
                                   imageMapService: ImageMapService,
                                   nutrientMappingService: NutrientMappingService,
                                   imageStorageService: ImageStorageService,
                                   deadbolt: DeadboltActionsAdapter) extends Controller with DatabaseErrorHandler with UpickleUtil {

  import uk.ac.ncl.openlab.intake24.errors.ErrorUtils._

  def getCategoryContents(code: String, locale: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(foodBrowsingService.getCategoryContents(code, locale).right.map {
          contents => LookupResult(contents.foods, contents.subcategories)
        })
      }
  }

  def getRootCategories(locale: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(foodBrowsingService.getRootCategories(locale))
      }
  }

  private def forSurvey(psm: PortionSizeMethod) = {
    val parametersMap = psm.parameters.foldLeft(Map[String, String]()) {
      case (m, PortionSizeMethodParameter(name, value)) => m + (name -> value)
    }

    val resolvedImageUrl = imageStorageService.getUrl(psm.imageUrl)

    PortionSizeMethodForSurvey(psm.method, psm.description, resolvedImageUrl, psm.useForRecipes, parametersMap)
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
          foodData.portionSizeMethods.map(forSurvey), associatedFoods, brands, categories)

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


  private def toAsServedSetWithUrls(set: UserAsServedSet) = {
    val images = set.images.map {
      image => UserAsServedImageWithUrls(imageStorageService.getUrl(image.mainImagePath), imageStorageService.getUrl(image.thumbnailPath), image.weight)
    }

    UserAsServedSetWithUrls(imageStorageService.getUrl(set.selectionImagePath), images)
  }

  def getAsServedSet(id: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(asServedImageService.getAsServedSet(id).right.map(toAsServedSetWithUrls))
      }
  }

  def getAsServedSets() = deadbolt.restrictToAuthenticated(upickleBodyParser[Seq[String]]) {
    request =>
      Future {
        translateDatabaseResult(sequence(request.body.map(asServedImageService.getAsServedSet(_))).right.map(_.map(toAsServedSetWithUrls)))
      }
  }

  private def toDrinkwareSetWithUrls(set: DrinkwareSet) = {
    val scales = set.scales.map {
      scale =>
        UserDrinkScaleWithUrls(scale.objectId, imageStorageService.getUrl(scale.baseImagePath), imageStorageService.getUrl(scale.overlayImagePath), scale.width, scale.height, scale.emptyLevel, scale.fullLevel, scale.volumeSamples)
    }

    UserDrinkwareSetWithUrls(set.guideId, scales)
  }

  def getDrinkwareSet(id: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(drinkwareService.getDrinkwareSet(id).right.map(toDrinkwareSetWithUrls))
      }
  }

  private def toImageMapWithUrls(imageMap: UserImageMap) = {
    val objects = imageMap.objects.map {
      o => UserImageMapObjectWithUrls(o.id, o.description, imageStorageService.getUrl(o.overlayPath), o.outline)
    }
    UserImageMapWithUrls(imageStorageService.getUrl(imageMap.baseImagePath), objects)
  }

  private def toGuideImageWithUrls(guideImage: UserGuideImage, imageMap: UserImageMap) = {
    UserGuideImageWithUrls(guideImage.description, toImageMapWithUrls(imageMap), guideImage.weights.map { case (k, v) => (k.toString, v) })
  }

  def getGuideImage(id: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        val result = for (
          guideImage <- guideImageService.getGuideImage(id).right;
          imageMap <- imageMapService.getImageMap(guideImage.imageMapId).right
        ) yield toGuideImageWithUrls(guideImage, imageMap)

        translateDatabaseResult(result)
      }
  }

  def getImageMap(id: String) = deadbolt.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(imageMapService.getImageMap(id).right.map(toImageMapWithUrls))
      }
  }

  def getImageMaps() = deadbolt.restrictToAuthenticated(upickleBodyParser[Seq[String]]) {
    request =>
      Future {
        translateDatabaseResult(imageMapService.getImageMaps(request.body).right.map(_.map(toImageMapWithUrls)))
      }
  }
}
