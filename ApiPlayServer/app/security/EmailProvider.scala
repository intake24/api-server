package security

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider

import scala.concurrent.ExecutionContext

@Singleton
class EmailProvider @Inject()(authInfoRepository: AuthInfoRepository, passwordHasherRegistry: PasswordHasherRegistry)(implicit executionContext: ExecutionContext) extends CredentialsProvider(authInfoRepository, passwordHasherRegistry) {
  override def id: String = EmailProvider.ID
}


object EmailProvider {
  val ID = "email"
}