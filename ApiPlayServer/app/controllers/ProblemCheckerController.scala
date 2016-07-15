package controllers

import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import play.api.mvc.Controller
import uk.ac.ncl.openlab.intake24.services.util.Timing
import modules.ProblemCheckerService
import play.api.mvc.Action
import upickle.default._
import play.api.http.ContentTypes

class ProblemCheckerController @Inject() (service: ProblemCheckerService, deadbolt: DeadboltActions) extends Controller {

  val maxReturnedProblems = 10

  def checkFood(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {
      Ok(write(service.foodProblems(code, locale))).as(ContentTypes.JSON)
    }
  }

  def checkCategory(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {

      Ok(write(service.categoryProblems(code, locale))).as(ContentTypes.JSON)
    }
  }

  def checkCategoryRecursive(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {

      Ok(write(service.recursiveCategoryProblems(code, locale, maxReturnedProblems))).as(ContentTypes.JSON)
    }
  }
}
