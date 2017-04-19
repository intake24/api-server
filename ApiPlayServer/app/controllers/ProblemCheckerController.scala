package controllers

import javax.inject.Inject

import io.circe.generic.auto._
import modules.ProblemCheckerService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

import scala.concurrent.Future

class ProblemCheckerController @Inject() (service: ProblemCheckerService, deadbolt: DeadboltActionsAdapter) extends Controller with DatabaseErrorHandler {

  val maxReturnedProblems = 10

  def checkFood(code: String, locale: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getFoodProblems(code, locale))
    }
  }

  def checkCategory(code: String, locale: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getCategoryProblems(code, locale))
    }
  }

  def checkCategoryRecursive(code: String, locale: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getRecursiveCategoryProblems(code, locale, maxReturnedProblems))
    }
  }
}
