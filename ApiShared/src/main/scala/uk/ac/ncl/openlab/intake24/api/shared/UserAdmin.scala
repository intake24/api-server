package uk.ac.ncl.openlab.intake24.api.shared

import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserInfoWithId

case class DeleteUsersRequest(userIds: Seq[Int])

case class CreateUsersRequest(newUsers: Seq[UserInfoWithId])

case class UserRecordWithPermissions(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String], roles: Set[String], permissions: Set[String]) {
  def withoutPermissions = UserRecord(userName, password, name, email, phone, customFields)
}

case class CreateOrUpdateGlobalUsersRequest(userRecords: Seq[UserRecordWithPermissions])

case class CreateOrUpdateUsersRequest(userRecords: Seq[UserRecord])

case class UserRecord(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String])
