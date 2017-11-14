package controllers

import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import parsers.JsonBodyParser
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.errors.FailedValidation
import uk.ac.ncl.openlab.intake24.services.systemdb.uxEvents.{UxEventIn, UxEventOut, UxEventsDataService}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Tim Osadchiy on 08/11/2017.
  */

case class EmitRequest(eventType: String, eventCategories: Seq[String], sessionId: UUID, localTime: Long, data: Json) {
  def toUxEvent(userId: Long) = new UxEventIn(eventCategories, eventType, data.noSpaces, userId, sessionId, localTime)
}

case class EmitResponse(id: Long, eventCategories: Seq[String], eventType: String, data: Json, created: ZonedDateTime)

object EmitResponse {
  def fromUxEvent(uxEvent: UxEventOut): Either[FailedValidation, EmitResponse] = parse(uxEvent.data) match {
    case Left(_) => Left(FailedValidation(new Exception("Could not parse JSON save in DB")))
    case Right(d) => Right(new EmitResponse(uxEvent.id, uxEvent.eventCategories, uxEvent.eventType, d, uxEvent.created))
  }
}

class UxEventsController @Inject()(foodAuthChecks: FoodAuthChecks,
                                   uxEventDataService: UxEventsDataService,
                                   rab: Intake24RestrictedActionBuilder,
                                   jsonBodyParser: JsonBodyParser,
                                   val controllerComponents: ControllerComponents,
                                   implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler {

  def dispatch() = rab.restrictToAuthenticated(jsonBodyParser.parse[EmitRequest]) {
    req =>
      Future {
        translateDatabaseResult(uxEventDataService.create(req.body.toUxEvent(req.subject.userId)))
      }
  }

}
