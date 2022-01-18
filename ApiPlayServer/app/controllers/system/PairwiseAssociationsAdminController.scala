package controllers.system

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import controllers.DatabaseErrorHandler
import play.api.mvc.{BaseController, ControllerComponents, Result}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.errors.{DatabaseError, StillReferenced}
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class PairwiseAssociationsAdminController @Inject()(paService: PairwiseAssociationsService,
                                                    paDataService: PairwiseAssociationsDataService,
                                                    rab: Intake24RestrictedActionBuilder,
                                                    val controllerComponents: ControllerComponents,
                                                    implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler {


  def update() = rab.restrictToRoles(Roles.surveyAdmin) {
    _ =>
      Future {
        handleResult(paService.update())
      }
  }

  def rebuild() = rab.restrictToRoles(Roles.surveyAdmin) {
    _ =>
      Future {
        handleResult(paService.rebuild())
      }
  }

  def copyOccurrenceData(srcLocale: String, destLocale: String) = rab.restrictToRoles(Roles.surveyAdmin) {
    _ =>
      val eitherT = for (_ <- EitherT(paDataService.copyOccurrenceData(srcLocale, destLocale));
                         _ <- EitherT(paDataService.copyCoOccurrenceData(srcLocale, srcLocale)))
        yield ()

      eitherT.value.map {
        case Left(error) => translateDatabaseError(error)
        case Right(_) => Ok
      }
  }

  private def handleResult(result: Future[Either[DatabaseError, Unit]]): Result =
    result.value match {
      case Some(Success(Left(StillReferenced(_)))) => TooManyRequests
      case Some(Success(Left(error))) => translateDatabaseError(error)
      case Some(Failure(exception)) => throw exception
      case _ => Accepted
    }
}
