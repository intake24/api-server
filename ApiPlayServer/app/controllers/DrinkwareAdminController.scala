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

import io.circe.generic.auto._
import parsers.{FormDataUtil, VolumeSampleData, VolumeSamplesCSVParser}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{BaseController, ControllerComponents, MultipartFormData, Result}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.data.{ErrorDescription, NewImageMapWithObjectsRequest}
import uk.ac.ncl.openlab.intake24.play.utils.JsonBodyParser
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{DrinkwareAdminService, ImageMapsAdminService}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageAdminService.WrapDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import uk.ac.ncl.openlab.intake24.{DrinkScale, DrinkwareSet, DrinkwareSetRecord}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class SlidingScaleData(objectId: Int, baseImage: FilePart[TemporaryFile], outline: FilePart[TemporaryFile])

case class SlidingScaleImages(base: SlidingScaleImageInfo, overlay: SlidingScaleOverlayInfo)

class DrinkwareAdminController @Inject()(service: DrinkwareAdminService,
                                         imageAdminService: ImageAdminService,
                                         imageMapService: ImageMapsAdminService,
                                         foodAuthChecks: FoodAuthChecks,
                                         rab: Intake24RestrictedActionBuilder,
                                         jsonBodyParser: JsonBodyParser,
                                         val controllerComponents: ControllerComponents,
                                         implicit val executionContext: ExecutionContext) extends BaseController
  with ImageOrDatabaseServiceErrorHandler with FormDataUtil {

  private val svgParser = new SVGImageMapParser()

  def listDrinkwareSets() = rab.restrictAccess(foodAuthChecks.canReadPortionSizeMethods) {
    Future {
      translateDatabaseResult(service.listDrinkwareSets())
    }
  }

  def getDrinkwareSet(id: String) = rab.restrictAccess(foodAuthChecks.canReadPortionSizeMethods) {
    Future {
      translateDatabaseResult(service.getDrinkwareSet(id))
    }
  }

  def createDrinkwareSetRecord() = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(jsonBodyParser.parse[DrinkwareSetRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createDrinkwareSetRecord(request.body))
      }
  }

  def parseScaleData(formData: MultipartFormData[TemporaryFile]): Either[Result, Seq[SlidingScaleData]] =
    getIntData("scaleCount", formData).flatMap {
      scaleCount =>
        val z: Either[Result, Seq[SlidingScaleData]] = Right(Seq())

        Range(0, scaleCount).foldLeft(z) {
          case (result, index) =>
            for (
              scales <- result;
              objectId <- getIntData(s"scaleObjectId[${index}]", formData);
              baseImageFile <- getFile(s"scaleImageFile[${index}]", formData);
              outlineFile <- getFile(s"scaleOutlineFile[${index}]", formData)
            ) yield
              scales :+ SlidingScaleData(objectId, baseImageFile, outlineFile)
        }
    }


  private def createImageMap(id: String, baseImageFile: FilePart[TemporaryFile], parsedImageMap: AWTImageMap, volumeSampleData: Map[Int, VolumeSampleData],
                             uploaderString: String): Either[Result, Unit] = {
    val generatedDescriptions = parsedImageMap.outlines.map { case (k, v) => (k.toString, volumeSampleData(k).objectDescription) };
    val params = NewImageMapWithObjectsRequest(id, s"Image map for drinkware set $id", generatedDescriptions);
    val result = ImageMapAdminUtils.createImageMap(imageAdminService, imageMapService, baseImageFile, Seq("cup", "glass", "mug", "drink"), params, parsedImageMap, uploaderString)
    translateImageServiceAndDatabaseError(result)
  }

  private def processSlidingScaleImage(setId: String, slidingScale: SlidingScaleData, uploaderString: String): Either[Result, SlidingScaleImages] = {
    ImageMapAdminUtils.parseOutlineSvg(slidingScale.outline).flatMap {
      outline =>
        val result = for (
          sourceImageRecord <- imageAdminService.uploadSourceImage(ImageAdminService.getSourcePathForDrinkScale(setId, slidingScale.baseImage.filename),
            slidingScale.baseImage.ref.path,
            slidingScale.baseImage.filename, Seq("cup", "glass", "mug", "drink"), uploaderString);
          imageInfo <- imageAdminService.processForSlidingScale(setId, sourceImageRecord.id);
          overlayDescriptor <- imageAdminService.generateSlidingScaleOverlay(setId, sourceImageRecord.id, outline)
        ) yield SlidingScaleImages(imageInfo, SlidingScaleOverlayInfo(overlayDescriptor, outline))

        translateImageServiceAndDatabaseError(result)
    }
  }

  private def processSlidingScaleImages(setId: String, slidingScales: Seq[SlidingScaleData], uploaderString: String): Either[Result, Seq[SlidingScaleImages]] = {
    val z: Either[Result, Seq[SlidingScaleImages]] = Right(Seq())

    slidingScales.foldLeft(z) {
      case (result, scale) =>
        for (list <- result;
             images <- processSlidingScaleImage(setId, scale, uploaderString))
        yield list :+ images
    }
  }

  private def validateScaleData(selectionMap: AWTImageMap, scaleData: Seq[SlidingScaleData]): Either[Result, Unit] = {
    val missingScales = selectionMap.outlines.keySet.filterNot(objectId => scaleData.exists(_.objectId == objectId))

    if (missingScales.size == 0)
      Right(())
    else
      Left(BadRequest(toJsonString(ErrorDescription("BadRequest", s"Sliding scale data is missing for object IDs: ${missingScales.mkString(", ")}"))))
  }

  private def makeLegacyRecord(setId: String, setDescription: String, scaleData: Seq[SlidingScaleData], scaleImages: Seq[SlidingScaleImages],
                               volumeSamples: Map[Int, VolumeSampleData]): Either[Result, DrinkwareSet] = {

    val missingVolumeData = scaleData.map(_.objectId).filterNot(objectId => volumeSamples.keySet.contains(objectId))

    if (missingVolumeData.size > 1)
      Left(BadRequest(toJsonString(ErrorDescription("BadRequest", s"Volume samples data is missing for object IDs: ${missingVolumeData.mkString(", ")}"))))
    else {
      val scales = scaleData.zip(scaleImages).map {
        case (data, images) =>
          val outlineBounds = images.overlay.outline.shape.getBounds2D

          val fullLevel = images.base.size.height - (images.base.size.height * outlineBounds.getMinY * images.overlay.outline.aspect).toInt
          val emptyLevel = images.base.size.height - (images.base.size.height * outlineBounds.getMaxY * images.overlay.outline.aspect).toInt

          DrinkScale(data.objectId, images.base.imageDescriptor.path, images.overlay.imageDescriptor.path, images.base.size.width, images.base.size.height,
            emptyLevel, fullLevel, volumeSamples(data.objectId).volumeSamples)
      }

      Right(DrinkwareSet(setId, setDescription, setId, scales))
    }
  }

  private def parseVolumeSamples(file: FilePart[TemporaryFile]): Either[Result, Map[Int, VolumeSampleData]] = {
    VolumeSamplesCSVParser.parseFile(file.ref.path.toFile) match {
      case Right(samples) => Right(samples)
      case Left(error) => Left(BadRequest(toJsonString(ErrorDescription("BadRequest", error))))
    }
  }

  def uploadDrinkwareSet() = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(parse.multipartFormData) {
    request =>

      val uploaderString = request.subject.userId.toString

      Future {
        val result = for (
          setId <- getData("setId", request.body);
          setDescription <- getData("setDescription", request.body);
          setImage <- getFile("setImageFile", request.body);
          setOutlines <- getFile("setOutlinesFile", request.body);
          volumeSamplesFile <- getFile("volumeSamplesFile", request.body);

          volumeSamples <- parseVolumeSamples(volumeSamplesFile);
          scaleData <- parseScaleData(request.body);
          imageMap <- ImageMapAdminUtils.parseImageMapSvg(setOutlines);
          _ <- validateScaleData(imageMap, scaleData);
          _ <- createImageMap(setId, setImage, imageMap, volumeSamples, uploaderString);
          scaleImages <- processSlidingScaleImages(setId, scaleData, uploaderString);
          legacyRecord <- makeLegacyRecord(setId, setDescription, scaleData, scaleImages, volumeSamples);
          _ <- translateImageServiceAndDatabaseError(service.createDrinkwareSets(Seq(legacyRecord)).wrapped)
        ) yield ()

        result match {
          case Left(badResult) => badResult
          case Right(()) => Ok
        }
      }
  }
}
