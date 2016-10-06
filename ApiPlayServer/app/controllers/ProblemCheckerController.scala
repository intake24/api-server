package controllers

import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import modules.ProblemCheckerService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.services.util.Timing
import upickle.default._
import security.Roles
import scala.concurrent.Future

class ProblemCheckerController @Inject() (service: ProblemCheckerService, deadbolt: DeadboltActionsAdapter) extends Controller with FoodDatabaseErrorHandler {

  val maxReturnedProblems = 10

  def checkFood(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.getFoodProblems(code, locale))
    }
  }

  def checkCategory(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.getCategoryProblems(code, locale))
    }
  }

  def checkCategoryRecursive(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.getRecursiveCategoryProblems(code, locale, maxReturnedProblems))
    }
  }
}
