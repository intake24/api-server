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

package controllers

import javax.inject.Inject

import io.circe.generic.auto._
import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.api.shared.NewGuideImageRequest
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{GuideImageAdminService, ImageMapsAdminService, NewGuideImageRecord}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageAdminService

import scala.concurrent.Future

class GuideImageAdminController @Inject()(guideImageAdminService: GuideImageAdminService, imageMapsAdminService: ImageMapsAdminService, imageAdminService: ImageAdminService, deadbolt: DeadboltActionsAdapter) extends Controller
  with ImageOrDatabaseServiceErrorHandler with JsonUtils {

  import ImageAdminService.WrapDatabaseError

  def listGuideImages() = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(guideImageAdminService.listGuideImages())
    }
  }

  def getGuideImage(id: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(guideImageAdminService.getGuideImage(id))
    }
  }

  def updateGuideSelectionImage(id: String, selectionImageId: Long) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(guideImageAdminService.updateGuideSelectionImage(id, selectionImageId))
    }
  }

  def createGuideImage() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[NewGuideImageRequest]) {
    request =>
      Future {

        val weights = request.body.objectWeights.map {
          case (k, v) => (k.toLong, v)
        }

        val result = for (
          sourceId <- imageMapsAdminService.getImageMapBaseImageSourceId(request.body.imageMapId).wrapped.right;
          selectionImageDescriptor <- imageAdminService.processForSelectionScreen(s"guide/${request.body.id}/selection", sourceId).right;
          _ <- guideImageAdminService.createGuideImages(Seq(NewGuideImageRecord(request.body.id, request.body.description, request.body.imageMapId, selectionImageDescriptor.id, weights))).wrapped.right) yield ()

        translateImageServiceAndDatabaseResult(result)
      }
  }
}
