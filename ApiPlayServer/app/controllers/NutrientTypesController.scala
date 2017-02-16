package controllers

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits._
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService
import security.DeadboltActionsAdapter
import play.api.mvc.Controller
import security.Roles

import scala.concurrent.Future

/**
  * Created by Tim Osadchiy on 16/02/2017.
  */
class NutrientTypesController @Inject()(nutrientService: NutrientMappingService,
                                        deadbolt: DeadboltActionsAdapter)
  extends Controller with FoodDatabaseErrorHandler{

  def list() = deadbolt.restrictAccess(Roles.superuser) {
    Future {
      translateDatabaseResult(nutrientService.supportedNutrients())
    }
  }

}
