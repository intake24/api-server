package uk.ac.ncl.openlab.intake24.api.client.test

import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.UserAdminClientImpl
import uk.ac.ncl.openlab.intake24.api.shared.{CreateOrUpdateGlobalUsersRequest, DeleteUsersRequest, UserRecordWithPermissions}

class UserAdminTest extends ApiTestSuite {

  val userAdminClient = new UserAdminClientImpl(apiBaseUrl)

  val users = Seq(
    UserRecordWithPermissions("test1", "123", Some("John Smith"), Some("john@smith.com"), Some("+4464783648372"), Map(), Set(), Set()),
    UserRecordWithPermissions("test2", "234", Some("Jack Black"), Some("jack@black.com"), Some("+4464732847238"), Map(), Set(), Set())
  )

  test("Create new global users") {
    val request = CreateOrUpdateGlobalUsersRequest(users)

    ensureSuccessful(userAdminClient.createOrUpdateGlobalUsers(accessToken, request))
  }

  test("List global users") {
    assert(ensureSuccessful(userAdminClient.listGlobalUsers(accessToken, 0, 100)).containsSlice(users))


  }

  test("Delete global users") {
    val users = Seq("test1", "test2")
    val request = DeleteUsersRequest(users)

    ensureSuccessful(userAdminClient.deleteGlobalUsers(accessToken, request))
  }

}
