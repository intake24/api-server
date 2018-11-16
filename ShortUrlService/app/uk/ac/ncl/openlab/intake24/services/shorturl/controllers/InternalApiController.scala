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

package uk.ac.ncl.openlab.intake24.services.shorturl.controllers

import io.circe.generic.auto._
import io.circe.syntax._
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.mvc.{BaseController, ControllerComponents}
import uk.ac.ncl.openlab.intake24.play.utils.{DatabaseErrorHandler, JsonBodyParser}
import uk.ac.ncl.openlab.intake24.services.shorturl.ShortUrlService
import uk.ac.ncl.openlab.intake24.shorturls.{ShortUrlsRequest, ShortUrlsResponse}

import scala.concurrent.ExecutionContext

class InternalApiController @Inject()(shortUrlService: ShortUrlService,
                                      val controllerComponents: ControllerComponents,
                                      jsonBodyParser: JsonBodyParser,
                                      implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler {

  val logger = LoggerFactory.getLogger(classOf[InternalApiController])

  def shorten() = Action.async(jsonBodyParser.parse[ShortUrlsRequest]) {
    request =>

      logger.debug(s"Shorten request: ${request.body.fullUrls.mkString(",")}")


      shortUrlService.getShortUrls(request.body.fullUrls).map {
        shortUrls =>

          logger.debug(s"Shorten result: ${shortUrls.mkString(",")}")

          Ok(ShortUrlsResponse(shortUrls).asJson.noSpaces)
      }
  }
}
