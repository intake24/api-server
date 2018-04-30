package controllers.foodSubstRecommender

import controllers.DatabaseErrorHandler
import javax.inject.Inject
import parsers.JsonUtils
import play.api.mvc.{BaseController, ControllerComponents}
import uk.ac.ncl.openlab.intake24.foodSubstRec.FoodSubstApi
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Tim Osadchiy on 26/04/2018.
  */
class FoodSubstRecommenderController @Inject()(foodSubstApi: FoodSubstApi,
                                               val controllerComponents: ControllerComponents,
                                               implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler with JsonUtils {

  def findSimilar(foodCode: String) = Action.async {
    _ =>
      Future {
        translateDatabaseResult(Right(foodSubstApi.findAlternatives(foodCode)))
      }
  }

  def supportedNutrients() = Action.async {
    _ =>
      Future {
        translateDatabaseResult(Right(foodSubstApi.listNutrients()))
      }
  }

  def defaultNutrients() = Action.async {
    _ =>
      Future {
        translateDatabaseResult(Right(foodSubstApi.defaultNutrientIds()))
      }
  }

}
