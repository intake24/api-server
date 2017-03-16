package apidocs

import java.time.Clock
import java.time.temporal.{ChronoField, TemporalField}
import java.util.UUID

import parsers.UpickleUtil
import uk.ac.ncl.openlab.intake24.api.shared._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{PublicUserRecord, PublicUserRecordWithPermissions}
import uk.ac.ncl.openlab.intake24.surveydata._

object DataExportExamples extends UpickleUtil {

  import JSONPrettyPrinter._

  val surveySubmissions = asPrettyJSON(Seq[ExportSubmission](
    ExportSubmission(UUID.fromString("2e90f16a-c8b7-4f68-9a3c-44e4b92ee807"), "user1", Map("userCustomField1" -> "123"), Map("surveyCustomField1" -> "456"),
      Clock.systemUTC().instant().minusSeconds(600).`with`(ChronoField.NANO_OF_SECOND, 0), Clock.systemUTC().instant().`with`(ChronoField.NANO_OF_SECOND, 0), Seq(ExportMeal("Breakfast", MealTime(8, 0), Map("mealCustomField1" -> "abc"), Seq(ExportFood(
        "FOOD001", "Example food", Some("Exemple alimentaire"), "good food", "NDNS", "1234", false, PortionSize("as-served", Map("servingWeight" -> "100", "leftoversWeight" -> "50")), true,
        10, "Some brand", Map(1 -> 5, 2 -> 10, 3 -> 14), Map("foodCustomField" -> "asdf"))
      ))))
  ))
}
