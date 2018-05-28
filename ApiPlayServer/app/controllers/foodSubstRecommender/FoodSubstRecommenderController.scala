package controllers.foodSubstRecommender

import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import javax.inject.Inject
import parsers.JsonUtils
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.foodSubstRec.FoodSubstApi
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDataService

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Tim Osadchiy on 26/04/2018.
  */
class FoodSubstRecommenderController @Inject()(foodSubstApi: FoodSubstApi,
                                               foodDataService: FoodDataService,
                                               rab: Intake24RestrictedActionBuilder,
                                               val controllerComponents: ControllerComponents,
                                               implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler with JsonUtils {

  def findSimilar(foodCode: String) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(foodSubstApi.findAlternatives(foodCode))
      }
  }

  def supportedNutrients() = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(Right(foodSubstApi.listNutrients()))
      }
  }

  def defaultNutrients() = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(Right(foodSubstApi.defaultNutrientIds()))
      }
  }

  def getFoodByHeader(foodCode: String, locale: String) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(foodDataService.getFoodHeader(foodCode, locale))
      }
  }

}
