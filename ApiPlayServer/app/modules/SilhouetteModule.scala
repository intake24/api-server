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

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.mohiva.play.silhouette.api.{Environment, EventBus}
import com.mohiva.play.silhouette.api.crypto.Base64AuthenticatorEncoder
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.impl.authenticators.{JWTAuthenticator, JWTAuthenticatorService, JWTAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import models.Intake24User
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import security.{AuthInfoServiceImpl, IdentityServiceImpl, Intake24ApiEnv, ShiroPasswordHasher}

import scala.concurrent.duration._


class SilhouetteModule extends AbstractModule with ScalaModule {

  def configure() {
    bind[IdentityService[Intake24User]].to[IdentityServiceImpl]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[Clock].toInstance(Clock())
    bind[AuthInfoRepository].to[AuthInfoServiceImpl]


    /* bind[CacheLayer].to[PlayCacheLayer]
    bind[EventBus].toInstance(EventBus())
    */
  }

  @Provides
  @Singleton
  @Named("refresh")
  def provideRefreshEnvironment(identityService: IdentityService[Intake24User],
                                @Named("refresh") authenticatorService: AuthenticatorService[JWTAuthenticator],
                                eventBus: EventBus): Environment[Intake24ApiEnv] = {
    Environment[Intake24ApiEnv](
      identityService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  @Singleton
  @Named("refresh")
  def provideRefreshAuthenticatorService(fingerprintGenerator: FingerprintGenerator,
                                         idGenerator: IDGenerator,
                                         configuration: Configuration,
                                         clock: Clock): AuthenticatorService[JWTAuthenticator] = {
    val settings = JWTAuthenticatorSettings(
      fieldName = "X-Auth-Token",
      issuerClaim = "intake24",
      requestParts = Some(Seq(RequestPart.Headers)),
      authenticatorIdleTimeout = None,
      authenticatorExpiry = configuration.getInt("intake24.security.refreshTokenExpiryDays").getOrElse(1825).days,
      sharedSecret = configuration.getString("play.crypto.secret").get)

    new JWTAuthenticatorService(settings, None, new Base64AuthenticatorEncoder(), idGenerator, clock)
  }

  @Provides
  @Singleton
  @Named("access")
  def provideAccessAuthenticatorService(fingerprintGenerator: FingerprintGenerator,
                                        idGenerator: IDGenerator,
                                        configuration: Configuration,
                                        clock: Clock): AuthenticatorService[JWTAuthenticator] = {

    val settings = JWTAuthenticatorSettings(
      fieldName = "X-Auth-Token",
      issuerClaim = "intake24",
      requestParts = Some(Seq(RequestPart.Headers)),
      authenticatorIdleTimeout = None,
      authenticatorExpiry = configuration.getInt("intake24.security.accessTokenExpiryMinutes").getOrElse(10).minutes,
      sharedSecret = configuration.getString("play.crypto.secret").get)

    new JWTAuthenticatorService(settings, None, new Base64AuthenticatorEncoder(), idGenerator, clock)
  }

  @Provides
  @Singleton
  @Named("access")
  def provideAccessEnvironment(identityService: IdentityService[Intake24User],
                               @Named("access") authenticatorService: AuthenticatorService[JWTAuthenticator],
                               eventBus: EventBus): Environment[Intake24ApiEnv] = {
    Environment[Intake24ApiEnv](
      identityService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }


  @Provides
  @Singleton
  def providePasswordHasherRegistry() = {
    val shiroHasher = new ShiroPasswordHasher()

    PasswordHasherRegistry(shiroHasher, Seq())
  }
}