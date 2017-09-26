package controllers

import javax.inject.Inject

import io.circe.generic.auto._
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.nutrition.FoodCompositionService

import scala.concurrent.{ExecutionContext, Future}

class NutrientTypesController @Inject()(service: FoodCompositionService,
                                        rab: Intake24RestrictedActionBuilder,
                                        val controllerComponents: ControllerComponents,
                                        implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler {

  def list() = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(service.getSupportedNutrients())
      }
  }

}
