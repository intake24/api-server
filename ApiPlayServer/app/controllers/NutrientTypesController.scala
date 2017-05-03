package controllers

import javax.inject.Inject

import io.circe.generic.auto._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.nutrition.FoodCompositionService

import scala.concurrent.Future

class NutrientTypesController @Inject()(service: FoodCompositionService,
                                        rab: Intake24RestrictedActionBuilder)
  extends Controller with DatabaseErrorHandler {

  def list() = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(service.getSupportedNutrients())
      }
  }

}
