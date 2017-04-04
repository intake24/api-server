package apidocs

import uk.ac.ncl.openlab.intake24.api.shared._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{PublicUserRecord, PublicUserRecordWithPermissions}


object UserManagementExamples {

  import JSONPrettyPrinter._

  val globalUserRecord = asPrettyJSON(Seq(PublicUserRecordWithPermissions("admin", Some("Super Admin"), Some("admin@admin.com"), None, Map(), Set("superuser"), Set())))

  val globalUserUpdate = asPrettyJSON(CreateOrUpdateGlobalUsersRequest(Seq(UserRecordWithPermissions("admin", "p455w0rd", Some("Super Admin"), None, None, Map(), Set("superuser"), Set()))))

  val globalUserDelete = asPrettyJSON(DeleteUsersRequest(Seq("user1", "user2")))

  val surveyUserRecord = asPrettyJSON(Seq(PublicUserRecord("user1", Some("John Smith"), None, None, Map("City" -> "Newcastle")),
    PublicUserRecord("user2", Some("Jack Black"), None, None, Map("City" -> "Cambridge"))))

  val surveyUserUpdate = asPrettyJSON(Seq(CreateOrUpdateSurveyUsersRequest(Seq(UserRecord("user1", "p455w0rd", Some("John Smith"), Some("john@smith.com"), Some("+441234567890"), Map())))))

  val surveyStaffRecord = asPrettyJSON(Seq(PublicUserRecord("staff1", Some("Jane Doe"), None, None, Map())))
}
