package security

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.{LoginInfo, Provider}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import uk.ac.ncl.openlab.intake24.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class URLTokenProvider @Inject()(userAdminService: UserAdminService) extends Provider {
  def id: String = URLTokenProvider.ID

  def authenticate(token: String): Future[LoginInfo] = Future {

    userAdminService.validateUrlToken(token) match {
      case Right(()) => LoginInfo(id, token)
      case Left(RecordNotFound(e)) => throw new IdentityNotFoundException("URL authentication token not recognized", e)
      case Left(e) => throw e.exception
    }
  }
}

object URLTokenProvider {
  val ID = "URLToken"
}