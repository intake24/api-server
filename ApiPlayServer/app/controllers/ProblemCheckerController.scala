package controllers

import javax.inject.Inject

import controllers.system.asynchronous.AsynchronousProblemsPrecacher
import io.circe.generic.auto._
import modules.ProblemCheckerService
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder

import scala.concurrent.{ExecutionContext, Future}

class ProblemCheckerController @Inject()(service: ProblemCheckerService,
                                         preacacher: AsynchronousProblemsPrecacher,
                                         foodAuthChecks: FoodAuthChecks,
                                         rab: Intake24RestrictedActionBuilder,
                                         val controllerComponents: ControllerComponents,
                                         implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler {

  val maxReturnedProblems = 10

  def checkFood(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoods(locale)) {
    if (preacacher.precacheFinished())
      Future {
        translateDatabaseResult(service.getFoodProblems(code, locale))
      }
    else
      Future.successful(ServiceUnavailable)
  }

  def checkCategory(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoods(locale)) {
    if (preacacher.precacheFinished())
      Future {
        translateDatabaseResult(service.getCategoryProblems(code, locale))
      }
    else
      Future.successful(ServiceUnavailable)
  }

  def checkCategoryRecursive(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoods(locale)) {
    if (preacacher.precacheFinished())
      Future {
        translateDatabaseResult(service.getRecursiveCategoryProblems(code, locale, maxReturnedProblems))
      }
    else
      Future.successful(ServiceUnavailable)
  }
}
