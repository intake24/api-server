package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, UserAdminClient}
import uk.ac.ncl.openlab.intake24.api.shared.{CreateOrUpdateGlobalUsersRequest, DeleteUsersRequest, UserRecordWithPermissions}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.PublicUserRecord

class UserAdminClientImpl(apiBaseUrl: String) extends UserAdminClient with HttpRequestUtil with ApiResponseParser {
  override def createOrUpdateGlobalUsers(accessToken: String, request: CreateOrUpdateGlobalUsersRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthPostRequest(s"$apiBaseUrl/admin/users/create-or-update", accessToken, request).asString)

  override def deleteGlobalUsers(accessToken: String, request: DeleteUsersRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthDeleteRequest(s"$apiBaseUrl/admin/users/delete", accessToken, request).asString)

  override def listGlobalUsers(accessToken: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecord]] =
    parseApiResponse[Seq[PublicUserRecord]](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/users", accessToken).param("offset", offset.toString).param("limit", offset.toString).asString)
}
