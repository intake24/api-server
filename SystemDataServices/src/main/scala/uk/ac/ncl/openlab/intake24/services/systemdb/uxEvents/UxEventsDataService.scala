package uk.ac.ncl.openlab.intake24.services.systemdb.uxEvents

import java.time.ZonedDateTime
import java.util.UUID

import uk.ac.ncl.openlab.intake24.errors.CreateError

/**
  * Created by Tim Osadchiy on 08/11/2017.
  */

case class UxEventIn(eventCategories: Seq[String], eventType: String, data: String, userId: Long, sessionId: UUID, localTimestamp: Long)

case class UxEventOut(id: Long, eventCategories: Seq[String], eventType: String, data: String, created: ZonedDateTime)

trait UxEventsDataService {
  def create(uxEvent: UxEventIn): Either[CreateError, Unit]

}
