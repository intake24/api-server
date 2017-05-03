package controllers.system.user

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Controller, Result}
import security.{AclService, DeadboltActionsAdapter}
import io.circe.generic.auto._
import models.{AccessSubject, Intake24Subject}
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
                                           aclService: AclService,
                                           deadbolt: DeadboltActionsAdapter)
  extends Controller with DatabaseErrorHandler with JsonUtils {

  private def doWithDatabaseCheck(check: Either[AnyError, Boolean])(block: => Result): Result =
    check match {
      case Right(true) => block
      case Right(false) => Forbidden
      case Left(e) => translateDatabaseError(e)
    }

  def getMyPhysicalData() = deadbolt.restrictToAuthenticated {
    request =>
      Future {
        val userId = request.subject.get.asInstanceOf[Intake24Subject].userId

        val result = userAdminService.getUserById(userId).right.flatMap {
          user => service.get(userId) match {
            case Right(userInfo) => Right(UserProfileWithPhysicalData(user, Some(userInfo)))
            case Left(RecordNotFound(_)) => Right(UserProfileWithPhysicalData(user, None))
            case Left(x) => Left(x)
          }
        }

        translateDatabaseResult(result)

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

  def patchUserPhysicalData(userId: Long) = deadbolt.restrictToAuthenticated(jsonBodyParser[UserPhysicalDataIn]) {
    request =>
      Future {
        val subject = request.subject.get.asInstanceOf[AccessSubject]

        doWithDatabaseCheck(aclService.canPatchUser(subject, userId)) {
          translateDatabaseResult(service.update(userId, request.body))
        }
      }
  }

}
