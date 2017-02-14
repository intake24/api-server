package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.shared.{CreateOrUpdateGlobalUsersRequest, CreateOrUpdateUsersRequest, DeleteUsersRequest}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{PublicUserRecord, PublicUserRecordWithPermissions}

trait UserAdminClient {

  def listGlobalUsers(accessToken: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecordWithPermissions]]

  def createOrUpdateGlobalUsers(accessToken: String, request: CreateOrUpdateGlobalUsersRequest): Either[ApiError, Unit]

  def deleteGlobalUsers(accessToken: String, request: DeleteUsersRequest): Either[ApiError, Unit]

  def createOrUpdateSurveyStaff(accessToken: String, surveyId: String, request: CreateOrUpdateUsersRequest): Either[ApiError, Unit]

  def uploadSurveyStaffCSV(accessToken: String, surveyId: String, csvFilePath: String): Either[ApiError, Unit]

  def listSurveyStaff(accessToken: String, surveyId: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecord]]

  def createOrUpdateSurveyRespondents(accessToken: String, surveyId: String, request: CreateOrUpdateUsersRequest): Either[ApiError, Unit]

  def uploadSurveyRespondentsCSV(accessToken: String, surveyId: String, csvFilePath: String): Either[ApiError, Unit]

  def listSurveyRespondents(accessToken: String, surveyId: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecord]]

  def deleteSurveyUsers(accessToken: String, surveyId: String, request: DeleteUsersRequest): Either[ApiError, Unit]
}
