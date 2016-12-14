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

import java.nio.file.Paths
import javax.inject.Inject

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import upickle.default._

import scala.concurrent.Future

class ImageMapAdminController @Inject()(
                                         imageMaps: ImageMapsAdminService,
                                         imageDatabase: ImageDatabaseService,
                                         imageAdmin: ImageAdminService,
                                         imageStorage: ImageStorageService,
                                         deadbolt: DeadboltActionsAdapter) extends Controller
  with ImageOrDatabaseServiceErrorHandler {

  import ImageAdminService.{WrapImageServiceError, WrapDatabaseError}

  private val svgParser = new SVGImageMapParser()

  /* def resolveUrls(image: AsServedImageWithPaths): AsServedImageWithUrls =
    AsServedImageWithUrls(image.sourceId, imageStorage.getUrl(image.imagePath), imageStorage.getUrl(image.thumbnailPath), image.weight)

  def resolveUrls(set: AsServedSetWithPaths): AsServedSetWithUrls = AsServedSetWithUrls(set.id, set.description, set.images.map(resolveUrls))*/

  private case class NewImageMapRequest(id: String, description: String, objectDescriptions: Map[Int, String])

  def createImageMapFromSVG() = deadbolt.restrict(Roles.superuser)(parse.multipartFormData) {
    request =>
      Future {
        val baseImageOpt = request.body.files.find(_.key == "baseImage")
        val svgImageMapOpt = request.body.files.find(_.key == "SVGImageMap")
        val reqOpt = request.body.dataParts.get("request")

        if (baseImageOpt.isEmpty)
          BadRequest("""{"cause":"baseImage file is missing"}""")
        else if (svgImageMapOpt.isEmpty)
          BadRequest("""{"cause":"SVGImageMap file is missing"}""")
        else if (reqOpt.isEmpty)
          BadRequest("""{"cause":"request field is missing"}""")
        else {
          val baseImage = baseImageOpt.get
          val svgImageMap = svgImageMapOpt.get
          val req = read[NewImageMapRequest](reqOpt.get.head)
          val keywords = request.body.dataParts.get("keywords").getOrElse(Seq())

          val svgImageMapPath = svgImageMap.ref.file.getPath

          Logger.debug(s"Parsing SVG image map from ${svgImageMapPath}")

          val imageMap = svgParser.parseImageMap(svgImageMapPath)

          imageMap.outlines.keySet.find(!req.objectDescriptions.contains(_)) match {
            case Some(missingId) => BadRequest(s"""{"cause":"missing description for object $missingId"}""")
            case None => {
              val result = for (
                baseImageSourceId <- imageAdmin.uploadSourceImage(ImageAdminService.getSourcePathForImageMap(req.id, baseImage.filename), Paths.get(baseImage.ref.file.getPath), keywords, request.subject.get.identifier).right;
                processedBaseImageDescriptor <- imageAdmin.processForImageMapBase(req.id, baseImageSourceId).right;
                overlayDescriptors <- imageAdmin.generateImageMapOverlays(req.id, baseImageSourceId, imageMap).right;
                _ <- {

                  val objects = imageMap.outlines.keySet.foldLeft(Map[Int, ImageMapObjectRecord]()) {
                    case (acc, objectId) =>
                      acc + (objectId -> ImageMapObjectRecord(req.objectDescriptions(objectId), imageMap.getCoordsArray(objectId).toArray, overlayDescriptors(objectId).id))
                  }

                  imageMaps.createImageMaps(Seq(NewImageMapRecord(req.id, req.description, processedBaseImageDescriptor.id, imageMap.navigation, objects)))
                }.wrapped.right

              ) yield ()

              translateImageServiceAndDatabaseResult(result)
            }
          }
        }
      }
  }
}
