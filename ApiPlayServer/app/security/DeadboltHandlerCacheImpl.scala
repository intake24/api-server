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

package security

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.DeadboltHandler
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import javax.inject.Inject
import javax.inject.Singleton
import be.objectify.deadbolt.scala.HandlerKey

case object DefaultHandler extends HandlerKey

@Singleton
class DeadboltHandlerCacheImpl @Inject() (env: Environment[User, JWTAuthenticator]) extends HandlerCache {
    val defaultHandler: DeadboltHandler = new DeadboltHandlerImpl(env)

    // HandlerKeys is an user-defined object, containing instances of a case class that extends HandlerKey  
    val handlers: Map[Any, DeadboltHandler] = Map(DefaultHandler -> defaultHandler)

    // Get the default handler.
    override def apply(): DeadboltHandler = defaultHandler

    // Get a named handler
    override def apply(handlerKey: HandlerKey): DeadboltHandler = handlers(handlerKey)
}