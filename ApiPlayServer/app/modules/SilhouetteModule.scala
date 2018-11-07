/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.mohiva.play.silhouette.api.crypto.Base64AuthenticatorEncoder
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus}
import com.mohiva.play.silhouette.impl.authenticators.{JWTAuthenticator, JWTAuthenticatorService, JWTAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import models.Intake24User
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import security.{AuthInfoServiceImpl, IdentityServiceImpl, Intake24ApiEnv, ShiroPasswordHasher}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class SilhouetteModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[IdentityService[Intake24User]].to[IdentityServiceImpl]
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[Clock].toInstance(Clock())
    bind[AuthInfoRepository].to[AuthInfoServiceImpl]
  }

  @Provides
  def provideIDGenerator(executionContext: ExecutionContext): IDGenerator = new SecureRandomIDGenerator()(executionContext)

  @Provides
  @Singleton
  def provideAuthenticatorService(fingerprintGenerator: FingerprintGenerator,
                                  idGenerator: IDGenerator,
                                  configuration: Configuration,
                                  executionContext: ExecutionContext,
                                  clock: Clock): AuthenticatorService[JWTAuthenticator] = {

    val settings = JWTAuthenticatorSettings(
      fieldName = "X-Auth-Token",
      issuerClaim = "intake24",
      requestParts = Some(Seq(RequestPart.Headers)),
      authenticatorIdleTimeout = None,
      authenticatorExpiry = 0.minutes, // Will be overriden when the token is created
      sharedSecret = configuration.get[String]("play.http.secret.key"))

    new JWTAuthenticatorService(settings, None, new Base64AuthenticatorEncoder(), idGenerator, clock)(executionContext)
  }

  @Provides
  @Singleton
  def provideSilhouetteEnvironment(identityService: IdentityService[Intake24User],
                                   authenticatorService: AuthenticatorService[JWTAuthenticator],
                                   executionContext: ExecutionContext,
                                   eventBus: EventBus): Environment[Intake24ApiEnv] = {
    Environment[Intake24ApiEnv](
      identityService,
      authenticatorService,
      Seq(),
      eventBus
    )(executionContext)
  }


  @Provides
  @Singleton
  def providePasswordHasherRegistry() = {
    val shiroHasher = new ShiroPasswordHasher()

    PasswordHasherRegistry(shiroHasher, Seq())
  }
}