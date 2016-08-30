package controllers

import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import play.api.mvc.Controller
import uk.ac.ncl.openlab.intake24.services.util.Timing
import modules.ProblemCheckerService
import play.api.mvc.Action
import upickle.default._
import play.api.http.ContentTypes

class ProblemCheckerController @Inject() (service: ProblemCheckerService, deadbolt: DeadboltActions) extends Controller with ApiErrorHandler {

  val maxReturnedProblems = 10

  def checkFood(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {
      translateResult(service.getFoodProblems(code, locale))
    }
  }

  def checkCategory(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {
      translateResult(service.getCategoryProblems(code, locale))
    }
  }

  def checkCategoryRecursive(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {
      translateResult(service.getRecursiveCategoryProblems(code, locale, maxReturnedProblems))
    }
  }
}
