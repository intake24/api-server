package controllers

import javax.inject.Inject

import io.circe.generic.auto._
import modules.ProblemCheckerService
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder

import scala.concurrent.{ExecutionContext, Future}

class ProblemCheckerController @Inject()(service: ProblemCheckerService,
                                         foodAuthChecks: FoodAuthChecks,
                                         rab: Intake24RestrictedActionBuilder,
                                         val controllerComponents: ControllerComponents,
                                         implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler {

  val maxReturnedProblems = 10

  def checkFood(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoods(locale)) {
    Future {
      translateDatabaseResult(service.getFoodProblems(code, locale))
    }
  }

  def checkCategory(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoods(locale)) {
    Future {
      translateDatabaseResult(service.getCategoryProblems(code, locale))
    }
  }

  def checkCategoryRecursive(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoods(locale)) {
    Future {
      translateDatabaseResult(service.getRecursiveCategoryProblems(code, locale, maxReturnedProblems))
    }
  }
}
