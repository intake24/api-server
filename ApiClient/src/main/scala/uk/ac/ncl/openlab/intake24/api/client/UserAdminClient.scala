package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.shared.{CreateOrUpdateGlobalUsersRequest, DeleteUsersRequest}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.PublicUserRecord

trait UserAdminClient {

  def listGlobalUsers(accessToken: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecord]]

  def createOrUpdateGlobalUsers(accessToken: String, request: CreateOrUpdateGlobalUsersRequest): Either[ApiError, Unit]

  def deleteGlobalUsers(accessToken: String, request: DeleteUsersRequest): Either[ApiError, Unit]
}
