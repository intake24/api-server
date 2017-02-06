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

import javax.inject.{Inject, Singleton}

import be.objectify.deadbolt.scala.{DeadboltHandler, HandlerKey}
import be.objectify.deadbolt.scala.cache.HandlerCache
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Environment

case object RefreshHandler extends HandlerKey

case object AccessHandler extends HandlerKey

@Singleton
class DeadboltHandlerCacheImpl @Inject()(@Named("refresh") refreshEnv: Environment[Intake24ApiEnv], @Named("access") accessEnv: Environment[Intake24ApiEnv]) extends HandlerCache {
  val refreshHandler: DeadboltHandler = new DeadboltRefreshHandlerImpl(refreshEnv)
  val accessHandler: DeadboltHandler = new DeadboltAccessHandlerImpl(accessEnv)

  // HandlerKeys is an user-defined object, containing instances of a case class that extends HandlerKey
  val handlers: Map[Any, DeadboltHandler] = Map(AccessHandler -> accessHandler, RefreshHandler -> refreshHandler)

  // Get the default handler.
  override def apply(): DeadboltHandler = accessHandler

  // Get a named handler
  override def apply(handlerKey: HandlerKey): DeadboltHandler = handlers(handlerKey)
}