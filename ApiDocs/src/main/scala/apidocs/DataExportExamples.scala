package apidocs

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZonedDateTime}
import java.time.temporal.{ChronoField, TemporalField}
import java.util.UUID

import parsers.JsonUtils
import uk.ac.ncl.openlab.intake24.api.shared._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{ExportFood, ExportMeal, ExportSubmission}
import uk.ac.ncl.openlab.intake24.surveydata._
import upickle.Js

object DataExportExamples extends JsonUtils {

  import JSONPrettyPrinter._

  implicit val dateTimeWriter = new upickle.default.Writer[ZonedDateTime]() {
    def write0 = (t: ZonedDateTime) => Js.Str(t.format(DateTimeFormatter.ISO_DATE_TIME))
  }

  val surveySubmissions = asPrettyJSON(Seq[ExportSubmission](
    ExportSubmission(
      UUID.fromString("2e90f16a-c8b7-4f68-9a3c-44e4b92ee807"),
      123,
      Some("user1"),
      Map("userCustomField1" -> "123"), Map("surveyCustomField1" -> "456"),
      ZonedDateTime.now().minusHours(2),
      ZonedDateTime.now(),

      Seq(
        ExportMeal(
          "Breakfast",
          MealTime(8, 0),
          Map("mealCustomField1" -> "abc"),
          Seq(ExportFood(
            "FOOD001",
            "Example food",
            Some("Exemple alimentaire"),
            "good food",
            "NDNS",
            "1234",
            false,
            PortionSize("as-served", Map("servingWeight" -> "100", "leftoversWeight" -> "50")),
            true,
            10,
            "Some brand",
            Map(1 -> 5, 2 -> 10, 3 -> 14),
            Map("foodCustomField" -> "asdf")
          )),
          Seq[MissingFood](
            MissingFood(
              "Some food",
              "Dunno",
              "Something yellow",
              "Ate until it was gone",
              "Nope"
            )
          ))
      )
    )
  ))
}
