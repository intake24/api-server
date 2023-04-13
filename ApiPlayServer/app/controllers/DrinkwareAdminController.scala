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
import parsers.FormDataUtil
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{BaseController, ControllerComponents, MultipartFormData, Result}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.DrinkwareSetRecord
import uk.ac.ncl.openlab.intake24.api.data.NewImageMapWithObjectsRequest
import uk.ac.ncl.openlab.intake24.play.utils.JsonBodyParser
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{DrinkwareAdminService, ImageMapsAdminService}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{ImageAdminService, ImageDescriptor, SVGImageMapParser}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class SlidingScaleData(objectId: Int, baseImage: FilePart[TemporaryFile], outline: FilePart[TemporaryFile])

case class SlidingScaleImages(base: ImageDescriptor, overlay: ImageDescriptor)

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


  private def createImageMap(id: String, baseImageFile: FilePart[TemporaryFile], outlinesFile: FilePart[TemporaryFile], uploaderString: String): Either[Result, Unit] = {

    ImageMapAdminUtils.parseImageMapSvg(outlinesFile).flatMap {
      imageMap =>
        val generatedDescriptions = imageMap.outlines.map { case (k, v) => (k.toString, s"Vessel $k") };
        val params = NewImageMapWithObjectsRequest(id, s"Image map for drinkware set $id", generatedDescriptions);
        val result = ImageMapAdminUtils.createImageMap(imageAdminService, imageMapService, baseImageFile, Seq("cup", "glass", "mug", "drink"), params, imageMap, uploaderString)
        translateImageServiceAndDatabaseError(result)
    }
  }

  private def processSlidingScaleImage(setId: String, slidingScale: SlidingScaleData, uploaderString: String): Either[Result, SlidingScaleImages] = {
    ImageMapAdminUtils.parseOutlineSvg(slidingScale.outline).flatMap {
      outline =>
        val result = for (
          sourceImageRecord <- imageAdminService.uploadSourceImage(ImageAdminService.getSourcePathForDrinkScale(setId, slidingScale.baseImage.filename),
            slidingScale.baseImage.ref.path,
            slidingScale.baseImage.filename, Seq("cup", "glass", "mug", "drink"), uploaderString);
          processedImageDescriptor <- imageAdminService.processForSlidingScale(setId, sourceImageRecord.id);
          overlayDescriptor <- imageAdminService.generateSlidingScaleOverlay(setId, sourceImageRecord.id, outline)
        ) yield SlidingScaleImages(processedImageDescriptor, overlayDescriptor)

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

  def uploadDrinkwareSet() = rab.restrictAccess(foodAuthChecks.canWritePortionSizeMethods)(parse.multipartFormData) {
    request =>

      val uploaderString = request.subject.userId.toString

      Future {
        val result = for (
          setId <- getData("setId", request.body);
          setImage <- getFile("setImageFile", request.body);
          setOutlines <- getFile("setOutlinesFile", request.body);
          volumeSamples <- getFile("volumeSamplesFile", request.body);
          scaleData <- parseScaleData(request.body);

          _ <- createImageMap(setId, setImage, setOutlines, uploaderString);
          scaleImages <- processSlidingScaleImages(setId, scaleData, uploaderString)

        ) yield scaleImages.foreach {
          image =>
            println (s"Base image: (${image.base.id}) ${image.base.path}")
            println (s"Overlay: (${image.overlay.id}) ${image.overlay.path}")
        }

        result match {
          case Left(badResult) => badResult
          case Right(()) => Ok
        }
      }
  }
}
