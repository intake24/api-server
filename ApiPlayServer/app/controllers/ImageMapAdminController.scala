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
import parsers.FormDataUtil
import play.api.http.ContentTypes
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results.BadRequest
import play.api.mvc.{BaseController, ControllerComponents, Result}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.data.{ErrorDescription, ImageMapResponse, NewImageMapRequest, NewImageMapWithObjectsRequest}
import uk.ac.ncl.openlab.intake24.play.utils.JsonUtils
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.images._

import scala.concurrent.{ExecutionContext, Future}


case class ImageMapObjectWithUrl(description: String, overlayImageUrl: String, navigationIndex: Int, outlineCoordinates: Array[Double])

case class ImageMapWithUrls(id: String, description: String, baseImageId: Long, baseImageUrl: String, objects: Map[Int, ImageMapObjectWithUrl])

object ImageMapAdminUtils extends JsonUtils {

  private val svgParser = new SVGImageMapParser()

  import ImageAdminService.WrapDatabaseError

  def parseImageMapSvg(svgFile: FilePart[TemporaryFile]): Either[Result, AWTImageMap] =
    svgParser.parseImageMap(svgFile.ref.path.toString) match {
      case Left(e) => Left(BadRequest(toJsonString(ErrorDescription("InvalidParameter", s"Failed to parse the SVG image map: ${e.getClass.getName}: ${e.getMessage}"))))
      case Right(m) => Right(m)
    }

  def parseOutlineSvg(svgFile: FilePart[TemporaryFile]): Either[Result, AWTOutline] =
    svgParser.parseOutline(svgFile.ref.path.toString) match {
      case Left(e) => Left(BadRequest(toJsonString(ErrorDescription("InvalidParameter", s"Failed to parse the SVG outline: ${e.getClass.getName}: ${e.getMessage}"))))
      case Right(m) => Right(m)
    }

  def validateParams(params: NewImageMapWithObjectsRequest, parsedImageMap: AWTImageMap): Either[Result, Unit] =
    parsedImageMap.outlines.keySet.find(k => !params.objectDescriptions.contains(k.toString)) match {
      case Some(missingId) => Left(BadRequest(toJsonString(ErrorDescription("InvalidParameter", s"Missing description for object $missingId"))))
      case None => Right(())
    }

  def createImageMap(imageAdmin: ImageAdminService,
                     imageMaps: ImageMapsAdminService,
                     baseImage: FilePart[TemporaryFile],
                     keywords: Seq[String],
                     params: NewImageMapWithObjectsRequest,
                     imageMap: AWTImageMap,
                     uploader: String): Either[ImageServiceOrDatabaseError, Unit] = {
    for (
      baseImageSourceRecord <- imageAdmin.uploadSourceImage(ImageAdminService.getSourcePathForImageMap(params.id, baseImage.filename), baseImage.ref.path,
        baseImage.filename, keywords, uploader).right;
      processedBaseImageDescriptor <- imageAdmin.processForImageMapBase(params.id, baseImageSourceRecord.id).right;
      overlayDescriptors <- imageAdmin.generateImageMapOverlays(params.id, baseImageSourceRecord.id, imageMap).right;
      _ <- {

        val objects = imageMap.outlines.keySet.foldLeft(Map[Int, NewImageMapObject]()) {
          case (acc, objectId) =>
            acc + (objectId -> NewImageMapObject(objectId, params.objectDescriptions(objectId.toString), imageMap.navigation.indexOf(objectId), imageMap.getCoordsArray(objectId).toArray, overlayDescriptors(objectId).id))
        }

        imageMaps.createImageMaps(Seq(NewImageMapRecord(params.id, params.description, processedBaseImageDescriptor.id, imageMap.navigation, objects)))
      }.wrapped.right) yield ()
  }
}

class ImageMapAdminController @Inject()(
                                         imageMaps: ImageMapsAdminService,
                                         imageDatabase: ImageDatabaseService,
                                         imageAdmin: ImageAdminService,
                                         imageStorage: ImageStorageService,
                                         foodAuthChecks: FoodAuthChecks,
                                         rab: Intake24RestrictedActionBuilder,
                                         val controllerComponents: ControllerComponents,
                                         implicit val executionContext: ExecutionContext) extends BaseController
  with ImageOrDatabaseServiceErrorHandler with FormDataUtil {

  import ImageAdminService.WrapDatabaseError


  /* def resolveUrls(image: AsServedImageWithPaths): AsServedImageWithUrls =
    AsServedImageWithUrls(image.sourceId, imageStorage.getUrl(image.imagePath), imageStorage.getUrl(image.thumbnailPath), image.weight)

  def resolveUrls(set: AsServedSetWithPaths): AsServedSetWithUrls = AsServedSetWithUrls(set.id, set.description, set.images.map(resolveUrls))*/



  private def createImageMap(baseImage: FilePart[TemporaryFile], keywords: Seq[String], params: NewImageMapWithObjectsRequest, imageMap: AWTImageMap, uploader: String): Either[Result, Unit] =
    translateImageServiceAndDatabaseError(ImageMapAdminUtils.createImageMap(imageAdmin, imageMaps, baseImage, keywords, params, imageMap, uploader))

  private def createImageMapWithoutObjects(baseImage: FilePart[TemporaryFile], keywords: Seq[String], params: NewImageMapRequest, uploader: String): Either[Result, ImageMapResponse] =
    translateImageServiceAndDatabaseError(
      for (
        baseImageSourceRecord <- imageAdmin.uploadSourceImage(ImageAdminService.getSourcePathForImageMap(params.id, baseImage.filename),
          baseImage.ref.path, baseImage.filename, keywords, uploader).right;
        processedBaseImageDescriptor <- imageAdmin.processForImageMapBase(params.id, baseImageSourceRecord.id).right;
        _ <- imageMaps.createImageMaps(Seq(NewImageMapRecord(params.id, params.description, processedBaseImageDescriptor.id, Nil, Map.empty))).wrapped.right
      ) yield ImageMapResponse(params.id, params.description, baseImageSourceRecord.path))

  def listImageMaps() = rab.restrictAccess(foodAuthChecks.canReadPortionSizeMethods) {
    _ =>
      Future {
        translateDatabaseResult(imageMaps.listImageMaps())
      }
  }

  def getImageMap(id: String) = rab.restrictAccess(foodAuthChecks.canReadPortionSizeMethods) {
    _ =>
      Future {
        val result = imageMaps.getImageMap(id).map {
          imageMap =>

            val objects = imageMap.objects.foldLeft(Map[Int, ImageMapObjectWithUrl]()) {
              case (result, obj) =>
                result + (obj.id -> ImageMapObjectWithUrl(obj.description, imageStorage.getUrl(obj.overlayImagePath), obj.navigationIndex, obj.outlineCoordinates))
            }

            ImageMapWithUrls(imageMap.id, imageMap.description, imageMap.baseImageId, imageStorage.getUrl(imageMap.baseImagePath), objects)
        }

        translateDatabaseResult(result)
      }
  }

  def getImageMapBaseImageSourceId(id: String) = rab.restrictAccess(foodAuthChecks.canReadPortionSizeMethods) {
    _ =>
      Future {
        translateDatabaseResult(imageMaps.getImageMapBaseImageSourceId(id))
      }
  }

  def createImageMapFromSVG() = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(parse.multipartFormData) {
    request =>
      Future {
        val result = for (
          baseImage <- getFile("baseImage", request.body).right;
          svgImage <- getFile("svg", request.body).right;
          sourceKeywords <- getOptionalMultipleData("baseImageKeywords", request.body).right;
          params <- getParsedData[NewImageMapWithObjectsRequest]("imageMapParameters", request.body).right;
          imageMap <- ImageMapAdminUtils.parseImageMapSvg(svgImage).right;
          _ <- ImageMapAdminUtils.validateParams(params, imageMap).right;
          _ <- createImageMap(baseImage, sourceKeywords, params, imageMap, request.subject.userId.toString).right // FIXME: better uploader string
        ) yield ()

        result match {
          case Left(badResult) => badResult
          case Right(()) => Ok
        }
      }
  }

  def uploadImageMap() = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(parse.multipartFormData) {
    request =>
      Future {
        val result = for (
          baseImage <- getFile("baseImage", request.body);
          sourceKeywords <- getOptionalMultipleData("baseImageKeywords", request.body);
          params <- getParsedData[NewImageMapRequest]("imageMapParameters", request.body);
          imgMap <- createImageMapWithoutObjects(baseImage, sourceKeywords, params, request.subject.userId.toString) // FIXME: better uploader string
        ) yield imgMap

        result match {
          case Left(badResult) => badResult
          case Right(r) => Ok(toJsonString(r)).as(ContentTypes.JSON)
        }
      }
  }
}
