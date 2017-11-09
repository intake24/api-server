package uk.ac.ncl.openlab.intake24.services.systemdb.uxEvents

import java.time.ZonedDateTime

import uk.ac.ncl.openlab.intake24.errors.CreateError

/**
  * Created by Tim Osadchiy on 08/11/2017.
  */

case class UxEventIn(eventCategories: Seq[String], eventType: String, data: String)

case class UxEventOut(id: Long, eventCategories: Seq[String], eventType: String, data: String, created: ZonedDateTime)

object UxEvent {

  def apply(id: Long, eventCategories: Seq[String], eventName: String, data: String, created: ZonedDateTime): UxEventOut =
    new UxEventOut(id, eventCategories, eventName, data, created)

  def apply(eventCategories: Seq[String], eventName: String, data: String): UxEventIn =
    new UxEventIn(eventCategories, eventName, data)
}

trait UxEventsDataService {
  def create(uxEvent: UxEventIn): Either[CreateError, UxEventOut]
}
