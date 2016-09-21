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

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.api.Environment
import models.User
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.api.services.IdentityService
import security.IdentityServiceImpl
import net.codingwell.scalaguice.ScalaModule
import com.mohiva.play.silhouette.api.util.IDGenerator
import com.mohiva.play.silhouette.impl.util.SecureRandomIDGenerator
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.util.BCryptPasswordHasher
import com.mohiva.play.silhouette.api.util.FingerprintGenerator
import com.mohiva.play.silhouette.impl.util.DefaultFingerprintGenerator
import play.api.Configuration
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticatorSettings
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticatorService
import scala.concurrent.duration._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import security.AuthInfoServiceImpl
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import security.ShiroPasswordHasher


class SilhouetteModule extends AbstractModule with ScalaModule {

  def configure() {
    bind[IdentityService[User]].to[IdentityServiceImpl]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[Clock].toInstance(Clock())
    bind[AuthInfoRepository].to[AuthInfoServiceImpl]
    
    /* bind[CacheLayer].to[PlayCacheLayer]
    bind[EventBus].toInstance(EventBus())
    */
  }

  @Provides
  def provideEnvironment(
    identityService: IdentityService[User],
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus): Environment[User, JWTAuthenticator] = {

    Environment[User, JWTAuthenticator](
      identityService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  def provideAuthenticatorService(
    fingerprintGenerator: FingerprintGenerator,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[JWTAuthenticator] = {

    val settings = JWTAuthenticatorSettings(
      headerName = "X-Auth-Token",
      issuerClaim = "intake24",
      encryptSubject = true,
      authenticatorIdleTimeout = Some(20.minutes),
      authenticatorExpiry = 12.hours,
      sharedSecret = configuration.getString("play.crypto.secret").get)
    
      new JWTAuthenticatorService(settings, None, idGenerator, clock)    
  }

  /**
   * Provides the credentials provider.
   *
   * @param authInfoRepository The auth info repository implementation.
   * @param passwordHasher The default password hasher implementation.
   * @return The credentials provider.
   */
  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository): CredentialsProvider = {

    val shiroHasher = new ShiroPasswordHasher
    
    new CredentialsProvider(authInfoRepository, shiroHasher, Seq(shiroHasher))
  }
}