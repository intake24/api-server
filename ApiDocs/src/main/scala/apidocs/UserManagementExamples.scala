package apidocs


import java.time.LocalDate
import java.time.format.DateTimeFormatter

import uk.ac.ncl.openlab.intake24.api.data._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{NewRespondentIds, NewRespondentWithPhysicalData, UserProfile, UserProfileUpdate}
import upickle.Js


object UserManagementExamples {

  import JSONPrettyPrinter._

  val globalUserRecord = asPrettyJSON(
    Seq
    (UserProfile(
      123,
      Some("Super Admin"),
      Some("admin@admin.com"),
      None,
      true,
      true,
      Set("superuser"),
      Map()
    ))
  )

  val globalUserUpdate = asPrettyJSON(UserProfileUpdate(Some("Super Admin"),
    Some("admin@admin.com"),
    None,
    true,
    false
  ))


  val globalUserDelete = asPrettyJSON(DeleteUsersRequest(Seq(1l, 2l, 3l)))

  val surveyUserRecord = asPrettyJSON(Seq(
    UserInfoWithSurveyUserName(1l, "user1", "http://blah", Some("John Smith"), None, None, true, true, Set("respondent"), Map("City" -> "Newcastle")),
    UserInfoWithSurveyUserName(2l, "user2", "http://blah", Some("Jack Black"), None, None, true, true, Set("respondent"), Map("City" -> "Cambridge"))))

  val surveyUserUpdate = asPrettyJSON(Seq(CreateOrUpdateSurveyUsersRequest(Seq(NewRespondent("user1", "p455w0rd", Some("John Smith"), Some("john@smith.com"), Some("+441234567890"), Map())))))


  implicit val dateWriter = new upickle.default.Writer[LocalDate]() {
    def write0 = (d: LocalDate) => Js.Str(DateTimeFormatter.ISO_DATE.format(d))
  }

  val createRespondentsWithPhysicalDataRequest = asPrettyJSON(CreateRespondentsWithPhysicalDataRequest(
    Seq(
      NewRespondentWithPhysicalData(
        "john@smith.com",
        Some("John Smith"),
        Some("john@smith.com"),
        None,
        Some("m"),
        Some(LocalDate.parse("2000-01-01")),
        Some(80.0),
        Some("lose"),
        Some(180.0)
      )
    )
  ))

  val createRespondentsWithPhysicalDataResponse = asPrettyJSON(CreateRespondentsWithPhysicalDataResponse(
    Seq(
      NewRespondentIds(
        123l,
        "john@smith.com",
        "(URL-safe authentication token)"
      )
    )
  ))

}
