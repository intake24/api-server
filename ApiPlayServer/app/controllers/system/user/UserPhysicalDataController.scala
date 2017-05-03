package controllers.system.user

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import io.circe.generic.auto._
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{UserPhysicalDataIn, UserPhysicalDataService}

import scala.concurrent.Future

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */
class UserPhysicalDataController @Inject()(service: UserPhysicalDataService,
                                           rab: Intake24RestrictedActionBuilder)
  extends Controller with DatabaseErrorHandler with JsonUtils {

  def getMyPhysicalData() = rab.restrictToAuthenticated {
    request =>
      Future {
        val userId = request.subject.userId
        translateDatabaseResult(service.get(userId))
      }
  }

  def updateMyPhysicalData() = rab.restrictToAuthenticated(jsonBodyParser[UserPhysicalDataIn]) {
    request =>
      Future {
        val userId = request.subject.userId
        val userInfo = request.body
        translateDatabaseResult(service.update(userId, userInfo))
      }
  }

}
