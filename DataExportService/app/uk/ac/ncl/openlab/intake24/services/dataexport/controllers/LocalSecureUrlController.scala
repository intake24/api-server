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

package uk.ac.ncl.openlab.intake24.services.dataexport.controllers

import java.nio.file.{Files, Paths}

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.mvc.{BaseController, ControllerComponents}
import uk.ac.ncl.openlab.intake24.play.utils.DatabaseErrorHandler

import scala.collection.JavaConverters
import scala.concurrent.{ExecutionContext, Future}


class LocalSecureUrlController @Inject()(configuration: Configuration,
                                         val controllerComponents: ControllerComponents,
                                         implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler {

  val logger = LoggerFactory.getLogger(classOf[LocalSecureUrlController])

  val dirPath = Paths.get(configuration.get[String]("intake24.dataExport.secureUrl.local.directory"))

  def download(key: String) = Action.async {
    Future {
      JavaConverters.asScalaIterator(Files.newDirectoryStream(dirPath).iterator()).find(_.getFileName.toString.startsWith(key)) match {
        case Some(path) =>
          val clientName = path.getFileName.toString.drop(key.length + 1)
          Ok.sendFile(path.toFile, false, _ => clientName)
        case None =>
          NotFound
      }
    }
  }
}