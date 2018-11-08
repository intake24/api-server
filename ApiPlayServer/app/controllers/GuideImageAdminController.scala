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

import java.awt.Shape

import javax.inject.Inject
import io.circe.generic.auto._
import parsers.{FormDataUtil, JsonBodyParser}
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder

import scala.concurrent.{ExecutionContext, Future}
import uk.ac.ncl.openlab.intake24.api.data.NewGuideImageRequest
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{AWTImageMap, ImageAdminService, ImageStorageService, ShapeFactory}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.ImageMapService

case class AdminGuideImageHeader(id: String, description: String, imageMapId: String, baseImageUrl: String)

case class AdminGuideImageObject(id: Int, description: String, outlineCoordinates: Array[Double], overlayImageUrl: String, weight: Double)

case class AdminNewGuideImageObject(description: String, outlineCoordinates: Array[Double], weight: Double)

case class AdminGuideImage(id: String, description: String, imageMapId: String, baseImageUrl: String, objects: Seq[AdminGuideImageObject])

case class PatchGuideImageObjectsRequest(imageWidth: Double, imageHeight: Double, objects: Seq[AdminNewGuideImageObject])


class GuideImageAdminController @Inject()(guideImageAdminService: GuideImageAdminService,
                                          imageMapsService: ImageMapService,
                                          imageMapsAdminService: ImageMapsAdminService,
                                          imageAdminService: ImageAdminService,
                                          imageStorage: ImageStorageService,
                                          foodAuthChecks: FoodAuthChecks,
                                          rab: Intake24RestrictedActionBuilder,
                                          jsonBodyParser: JsonBodyParser,
                                          val controllerComponents: ControllerComponents,
                                          implicit val executionContext: ExecutionContext) extends BaseController
  with ImageOrDatabaseServiceErrorHandler with FormDataUtil {

  import ImageAdminService.WrapDatabaseError

  def listGuideImages() = rab.restrictAccess(foodAuthChecks.canReadPortionSizeMethods) {
    Future {

      val result = for (guideHeaders <- guideImageAdminService.listGuideImages();
                        imageMapHeaders <- imageMapsService.getImageMaps(guideHeaders.map(_.imageMapId))) yield {

        guideHeaders.zip(imageMapHeaders).map {
          case (guideHeader, imageMapHeader) =>
            AdminGuideImageHeader(guideHeader.id, guideHeader.description, guideHeader.imageMapId, imageStorage.getUrl(imageMapHeader.baseImagePath))
        }
      }
      translateDatabaseResult(result)
    }
  }

  def getGuideImage(id: String) = rab.restrictAccess(foodAuthChecks.canReadPortionSizeMethods) {
    Future {

      val result = for (guideImage <- guideImageAdminService.getGuideImage(id);
                        imageMap <- imageMapsAdminService.getImageMap(guideImage.imageMapId)) yield {
        val objects = imageMap.objects.sortBy(_.navigationIndex).map {
          obj =>
            AdminGuideImageObject(obj.id, obj.description, obj.outlineCoordinates,
              imageStorage.getUrl(obj.overlayImagePath), guideImage.weights(obj.id))
        }

        AdminGuideImage(id, guideImage.description, guideImage.imageMapId, imageStorage.getUrl(imageMap.baseImagePath), objects)
      }

      translateDatabaseResult(result)
    }
  }

  def patchGuideImageMeta(id: String) = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(jsonBodyParser.parse[GuideImageMeta]) {
    request =>
      Future {
        val result = for (
          result <- guideImageAdminService.updateGuideImageMeta(id, request.body);
          // this is to keep IDs and descriptions of image maps that are used for guide images in sync with guide image ids
          // at some point this needs to be dropped and moved to numeric IDs
          _ <- imageMapsAdminService.updateImageMapMeta(id, ImageMapMeta(request.body.id, request.body.description))
        ) yield result

        translateDatabaseResult(result)
      }
  }

  def patchGuideImageObjects(id: String) = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(jsonBodyParser.parse[PatchGuideImageObjectsRequest]) {
    request =>
      Future {

        // Object ID vs nav indexes aren't meaningful for guide images creating using the admin tool and could be
        // merged, however some legacy image maps have arbitrary object IDs that reflect numbers in the pictures
        // and they need to be able to be re-arranged

        val navIndexes = request.body.objects.indices
        val shapeMap = navIndexes.zip(request.body.objects.map(obj => ShapeFactory.getShapeFromFlatCoordinates(obj.outlineCoordinates))).toMap
        val imageMap = AWTImageMap(navIndexes, shapeMap, request.body.imageWidth / request.body.imageHeight)


        val result = for (
          imageMapId <- guideImageAdminService.getImageMapId(id).wrapped;
          sourceId <- imageMapsAdminService.getImageMapBaseImageSourceId(imageMapId).wrapped;
          overlays <- imageAdminService.generateImageMapOverlays(imageMapId, sourceId, imageMap);
          _ <- imageMapsAdminService.updateImageMapObjects(imageMapId, request.body.objects.zipWithIndex.map {
            case (obj, index) =>
              NewImageMapObject(index, obj.description, index, obj.outlineCoordinates, overlays(index).id)
          }).wrapped;
          _ <- guideImageAdminService.updateGuideImageObjects(id, request.body.objects.zipWithIndex.map {
            case (obj, index) =>
              GuideImageMapObject(index, obj.weight)
          }).wrapped
        ) yield ()

        translateImageServiceAndDatabaseResult(result)
      }
  }

  def updateGuideSelectionImage(id: String, selectionImageId: Long) = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods) {
    Future {
      translateDatabaseResult(guideImageAdminService.updateGuideSelectionImage(id, selectionImageId))
    }
  }

  def createGuideImage() = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(jsonBodyParser.parse[NewGuideImageRequest]) {
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
