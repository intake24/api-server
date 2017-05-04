package controllers.system.user

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import controllers.system.UserAuthChecks
import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Controller, Result}
import io.circe.generic.auto._
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.errors.{AnyError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{UserAdminService, UserProfileWithPhysicalData}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{UserPhysicalDataIn, UserPhysicalDataService}

import scala.concurrent.Future
import scala.util.{Left, Right}

/**
  * Created by Tim Osadchiy on 09/04/2017.
  */
class UserPhysicalDataController @Inject()(service: UserPhysicalDataService,
                                           userAdminService: UserAdminService,
                                           userAuthChecks: UserAuthChecks,
                                           rab: Intake24RestrictedActionBuilder)
  extends Controller with DatabaseErrorHandler with JsonUtils {

  private def doWithDatabaseCheck(check: Either[AnyError, Boolean])(block: => Result): Result =
    check match {
      case Right(true) => block
      case Right(false) => Forbidden
      case Left(e) => translateDatabaseError(e)
    }

  def getMyPhysicalData() = rab.restrictToAuthenticated {
    request =>
      Future {
        val userId = request.subject.userId

        val result = userAdminService.getUserById(userId).right.flatMap {
          user =>
            service.get(userId) match {
              case Right(userInfo) => Right(UserProfileWithPhysicalData(user, Some(userInfo)))
              case Left(RecordNotFound(_)) => Right(UserProfileWithPhysicalData(user, None))
              case Left(x) => Left(x)
            }
        }

        translateDatabaseResult(result)
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

  def patchUserPhysicalData(userId: Long) = rab.restrictAccessWithDatabaseCheck(userAuthChecks.canUpdateProfile(userId))(jsonBodyParser[UserPhysicalDataIn]) {
    request =>
      Future {
        translateDatabaseResult(service.update(userId, request.body))
      }
  }
}
