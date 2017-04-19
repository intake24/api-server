package controllers.system.user

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.{JsonUtils}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Controller}
import security.{DeadboltActionsAdapter}
import io.circe.generic.auto._
import models.Intake24Subject
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{UserPhysicalDataIn, UserPhysicalDataService}

import scala.concurrent.Future

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */
class UserPhysicalDataController @Inject()(service: UserPhysicalDataService,
                                           deadbolt: DeadboltActionsAdapter)
  extends Controller with DatabaseErrorHandler with JsonUtils {

  def getMyPhysicalData() = deadbolt.restrictToAuthenticated {
    request =>
      Future {
        val userId = request.subject.get.asInstanceOf[Intake24Subject].userId
        translateDatabaseResult(service.get(userId))
      }
  }

  def updateMyPhysicalData() = deadbolt.restrictToAuthenticated(jsonBodyParser[UserPhysicalDataIn]) {
    request =>
      Future {
        val userId = request.subject.get.asInstanceOf[Intake24Subject].userId
        val userInfo = request.body
        translateDatabaseResult(service.update(userId, userInfo))
      }
  }

}
