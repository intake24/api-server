package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import java.nio.file.Paths

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, UserAdminClient}
import uk.ac.ncl.openlab.intake24.api.shared.{CreateOrUpdateGlobalUsersRequest, CreateOrUpdateSurveyUsersRequest, DeleteUsersRequest}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{PublicUserRecord, PublicUserRecordWithPermissions}

class UserAdminClientImpl(apiBaseUrl: String) extends UserAdminClient with HttpRequestUtil with ApiResponseParser {
  override def createOrUpdateGlobalUsers(accessToken: String, request: CreateOrUpdateGlobalUsersRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthPostRequest(s"$apiBaseUrl/admin/users/create-or-update", accessToken, request).asString)

  override def deleteGlobalUsers(accessToken: String, request: DeleteUsersRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthDeleteRequest(s"$apiBaseUrl/admin/users/delete", accessToken, request).asString)

  override def listGlobalUsers(accessToken: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecordWithPermissions]] =
    parseApiResponse[Seq[PublicUserRecordWithPermissions]](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/users", accessToken).param("offset", offset.toString).param("limit", limit.toString).asString)

  override def uploadSurveyStaffCSV(accessToken: String, surveyId: String, csvFilePath: String): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthPostRequestForm(s"$apiBaseUrl/admin/users/$surveyId/staff/upload-csv", accessToken, Seq(), Seq(("csv", Paths.get(csvFilePath)))).asString)

  override def listSurveyStaff(accessToken: String, surveyId: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecord]] =
    parseApiResponse[Seq[PublicUserRecord]](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/users/$surveyId/staff", accessToken).param("offset", offset.toString).param("limit", limit.toString).asString)

  override def deleteSurveyUsers(accessToken: String, surveyId: String, request: DeleteUsersRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthDeleteRequest(s"$apiBaseUrl/admin/users/$surveyId/delete", accessToken, request).asString)

  override def uploadSurveyRespondentsCSV(accessToken: String, surveyId: String, csvFilePath: String): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthPostRequestForm(s"$apiBaseUrl/admin/users/$surveyId/respondents/upload-csv", accessToken, Seq(), Seq(("csv", Paths.get(csvFilePath)))).asString)

  override def listSurveyRespondents(accessToken: String, surveyId: String, offset: Int, limit: Int): Either[ApiError, Seq[PublicUserRecord]] =
    parseApiResponse[Seq[PublicUserRecord]](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/users/$surveyId/respondents", accessToken).param("offset", offset.toString).param("limit", limit.toString).asString)

  override def createOrUpdateSurveyStaff(accessToken: String, surveyId: String, request: CreateOrUpdateSurveyUsersRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthPostRequest(s"$apiBaseUrl/admin/users/$surveyId/staff/create-or-update", accessToken, request).asString)

  override def createOrUpdateSurveyRespondents(accessToken: String, surveyId: String, request: CreateOrUpdateSurveyUsersRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthPostRequest(s"$apiBaseUrl/admin/users/$surveyId/respondents/create-or-update", accessToken, request).asString)
}
