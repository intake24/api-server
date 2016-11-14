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

import play.api.Environment
import play.api.Configuration
import play.api.inject.Module
import play.api.inject.Binding
import be.objectify.deadbolt.scala.cache.HandlerCache
import security.DeadboltHandlerCacheImpl
import com.google.inject.AbstractModule
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageLocal
import com.google.inject.Injector
import uk.ac.ncl.openlab.intake24.services.fooddb.images.LocalImageStorageSettings
import com.google.inject.Provides
import com.google.inject.Singleton

class LocalStorageModule extends AbstractModule {

  @Provides
  @Singleton
  def settings(configuration: Configuration): LocalImageStorageSettings = 
    LocalImageStorageSettings(
        configuration.getString("intake24.images.localStorage.baseDirectory").get,
        configuration.getString("intake24.images.localStorage.urlPrefix").get) 
  
  def configure() = {
    bind(classOf[ImageStorageService]).to(classOf[ImageStorageLocal])
  }
}
