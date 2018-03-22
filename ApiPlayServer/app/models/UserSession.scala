package models

import uk.ac.ncl.openlab.intake24.services.systemdb.user.UserSession

/**
  * Created by Tim Osadchiy on 21/03/2018.
  */
case class SaveUserSessionRequest(data: String)

case class UserSessionResponse(data: Option[UserSession])
