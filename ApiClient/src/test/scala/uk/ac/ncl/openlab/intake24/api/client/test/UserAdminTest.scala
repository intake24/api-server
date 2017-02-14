package uk.ac.ncl.openlab.intake24.api.client.test

import org.scalatest.BeforeAndAfterAll
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.{SurveyAdminClientImpl, UserAdminClientImpl}
import uk.ac.ncl.openlab.intake24.api.shared._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{PublicUserRecord, PublicUserRecordWithPermissions}

import scala.util.Random

class UserAdminTest extends ApiTestSuite with RandomData with BeforeAndAfterAll {

  val userAdminClient = new UserAdminClientImpl(apiBaseUrl)
  val surveyAdminClient = new SurveyAdminClientImpl(apiBaseUrl)

  val customFieldNames = randomCustomFieldNames

  val newUsers = Seq.fill(Random.nextInt(9) + 1)(randomUserRecordWithPermission(customFieldNames))

  def toPublicUserRecord(r: UserRecordWithPermissions) =
    PublicUserRecordWithPermissions(r.userName, r.name, r.email, r.phone, r.customFields, r.roles, r.permissions)

  def toPublicUserRecord(r: UserRecord) =
    PublicUserRecord(r.userName, r.name, r.email, r.phone, r.customFields)

  val testSurveyId = randomString(10)

  var adminAccessToken: String = null

  override def beforeAll(): Unit = {
    val adminRefreshToken = assertSuccessful(signinClient.signin(Credentials("", "admin", "intake24"))).refreshToken
    adminAccessToken = assertSuccessful(signinClient.refresh(adminRefreshToken)).accessToken

    assertSuccessful(surveyAdminClient.createSurvey(adminAccessToken, CreateSurveyRequest(testSurveyId, "default", "enGB", false, None, "blah@blah.com")))
  }

  override def afterAll(): Unit = {
    assertSuccessful(surveyAdminClient.deleteSurvey(adminAccessToken, testSurveyId))

  }

  test("Create new global users") {
    val request = CreateOrUpdateGlobalUsersRequest(newUsers)

    assertSuccessful(userAdminClient.createOrUpdateGlobalUsers(adminAccessToken, request))
  }

  test("Check that all new global users exist") {
    val allUsers = assertSuccessful(userAdminClient.listGlobalUsers(adminAccessToken, 0, 100))

    newUsers.map(toPublicUserRecord).foreach {
      newUser =>
        assert(allUsers.contains(newUser))
    }
  }

  test("Delete global users") {
    val userNames = newUsers.map(_.userName)
    val request = DeleteUsersRequest(userNames)

    assertSuccessful(userAdminClient.deleteGlobalUsers(adminAccessToken, request))
  }

  test("Check that all new users have been deleted") {
    val allUsers = assertSuccessful(userAdminClient.listGlobalUsers(adminAccessToken, 0, 100))

    newUsers.map(toPublicUserRecord).foreach {
      newUser =>
        assert(!allUsers.contains(newUser))
    }
  }


  test("Create survey staff users") {
    val customFieldNames = randomCustomFieldNames

    val users = Seq.fill(Random.nextInt(10))(randomUserRecord(randomCustomFieldNames))

    val publicUsers = users.map(toPublicUserRecord)

    assertSuccessful(userAdminClient.createOrUpdateSurveyStaff(adminAccessToken, testSurveyId, CreateOrUpdateUsersRequest(users)))

    val userList = assertSuccessful(userAdminClient.listSurveyStaff(adminAccessToken, testSurveyId, 0, 100))

    publicUsers.foreach {
      user =>
        assert(userList.contains(user))
    }

    assertSuccessful(userAdminClient.deleteSurveyUsers(adminAccessToken, testSurveyId, DeleteUsersRequest(users.map(_.userName))))

    val postDeleteUserList = assertSuccessful(userAdminClient.listSurveyStaff(adminAccessToken, testSurveyId, 0, 100))

    publicUsers.foreach {
      user =>
        assert(!postDeleteUserList.contains(user))
    }
  }

  test("Upload survey staff CSV") {

    val (users, file) = randomUsersCSV

    assertSuccessful(userAdminClient.uploadSurveyStaffCSV(adminAccessToken, testSurveyId, file.getPath))

    val publicUserRecords = users.map {
      user =>
        PublicUserRecord(user.userName, user.name, user.email, user.phone, user.customFields)
    }

    val userList = assertSuccessful(userAdminClient.listSurveyStaff(adminAccessToken, testSurveyId, 0, 100))

    publicUserRecords.foreach {
      user =>
        assert(userList.contains(user))
    }

    assertSuccessful(userAdminClient.deleteSurveyUsers(adminAccessToken, testSurveyId, DeleteUsersRequest(users.map(_.userName))))

    val userListPostDelete = assertSuccessful(userAdminClient.listSurveyStaff(adminAccessToken, testSurveyId, 0, 100))

    publicUserRecords.foreach {
      user =>
        assert(!userListPostDelete.contains(user))
    }
  }

  test("Create survey respondent users") {
    val customFieldNames = randomCustomFieldNames

    val users = Seq.fill(Random.nextInt(10))(randomUserRecord(randomCustomFieldNames))

    val publicUsers = users.map(toPublicUserRecord)

    assertSuccessful(userAdminClient.createOrUpdateSurveyRespondents(adminAccessToken, testSurveyId, CreateOrUpdateUsersRequest(users)))

    val userList = assertSuccessful(userAdminClient.listSurveyRespondents(adminAccessToken, testSurveyId, 0, 100))

    publicUsers.foreach {
      user =>
        assert(userList.contains(user))
    }

    assertSuccessful(userAdminClient.deleteSurveyUsers(adminAccessToken, testSurveyId, DeleteUsersRequest(users.map(_.userName))))

    val postDeleteUserList = assertSuccessful(userAdminClient.listSurveyRespondents(adminAccessToken, testSurveyId, 0, 100))

    publicUsers.foreach {
      user =>
        assert(!postDeleteUserList.contains(user))
    }
  }

  test("Upload survey respondents CSV") {
    val (users, file) = randomUsersCSV

    assertSuccessful(userAdminClient.uploadSurveyRespondentsCSV(adminAccessToken, testSurveyId, file.getPath))

    val publicUserRecords = users.map {
      user =>
        PublicUserRecord(user.userName, user.name, user.email, user.phone, user.customFields)
    }

    val userList = assertSuccessful(userAdminClient.listSurveyRespondents(adminAccessToken, testSurveyId, 0, 100))

    publicUserRecords.foreach {
      user =>
        assert(userList.contains(user))
    }

    assertSuccessful(userAdminClient.deleteSurveyUsers(adminAccessToken, testSurveyId, DeleteUsersRequest(users.map(_.userName))))

    val userListPostDelete = assertSuccessful(userAdminClient.listSurveyRespondents(adminAccessToken, testSurveyId, 0, 100))

    publicUserRecords.foreach {
      user =>
        assert(!userListPostDelete.contains(user))
    }
  }

  val staffTestUserRecord = randomUserRecord(Seq())

  var staffUserAccessToken: String = null

  test("Create and sign in as survey staff") {
    assertSuccessful(userAdminClient.createOrUpdateSurveyStaff(adminAccessToken, testSurveyId, CreateOrUpdateUsersRequest(Seq(staffTestUserRecord))))
    val refreshToken = assertSuccessful(signinClient.signin(Credentials(testSurveyId, staffTestUserRecord.userName, staffTestUserRecord.password))).refreshToken
    staffUserAccessToken = assertSuccessful(signinClient.refresh(refreshToken)).accessToken
  }

  test("Try to create global users without rights") {
    assertForbidden(userAdminClient.createOrUpdateGlobalUsers(staffUserAccessToken, CreateOrUpdateGlobalUsersRequest(Seq(randomUserRecordWithPermission(Seq())))))
  }

  test("Try to delete global user without rights") {
    assertForbidden(userAdminClient.deleteGlobalUsers(staffUserAccessToken, DeleteUsersRequest(Seq("test"))))
  }

  test("Try to create users for another survey") {
    assertForbidden(userAdminClient.createOrUpdateSurveyStaff(staffUserAccessToken, "_no_such_survey", CreateOrUpdateUsersRequest(Seq())))
  }

  test("Try to delete users from another survey") {
    assertForbidden(userAdminClient.deleteSurveyUsers(staffUserAccessToken, "_no_such_survey", DeleteUsersRequest(Seq())))
  }

  test("Delete test staff account") {
    assertSuccessful(userAdminClient.deleteSurveyUsers(adminAccessToken, testSurveyId, DeleteUsersRequest(Seq(staffTestUserRecord.userName))))
  }

}
