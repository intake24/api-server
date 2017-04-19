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
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import io.circe.generic.auto._
import parsers.JsonUtils
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

import scala.concurrent.Future

case class AsServedImageWithUrls(sourceId: Long, imageUrl: String, thumbnailUrl: String, weight: Double)

case class AsServedSetWithUrls(id: String, description: String, images: Seq[AsServedImageWithUrls])

case class NewAsServedImage(sourceImageId: Long, weight: Double)

case class ExistingAsServedImage(processedImageId: Long, processedThumbnailId: Long, weight: Double)

case class NewAsServedSet(id: String, description: String, images: Seq[NewAsServedImage])

class AsServedSetsAdminController @Inject()(
                                             service: AsServedSetsAdminService,
                                             imageDatabase: ImageDatabaseService,
                                             imageAdmin: ImageAdminService,
                                             imageStorage: ImageStorageService,
                                             deadbolt: DeadboltActionsAdapter) extends Controller
  with ImageOrDatabaseServiceErrorHandler with JsonUtils {

  import ImageAdminService.WrapDatabaseError
  import uk.ac.ncl.openlab.intake24.errors.ErrorUtils.sequence

  def resolveUrls(image: AsServedImageWithPaths): AsServedImageWithUrls =
    AsServedImageWithUrls(image.sourceId, imageStorage.getUrl(image.imagePath), imageStorage.getUrl(image.thumbnailPath), image.weight)

  def resolveUrls(set: AsServedSetWithPaths): AsServedSetWithUrls = AsServedSetWithUrls(set.id, set.description, set.images.map(resolveUrls))

  def listAsServedSets() = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.listAsServedSets())
    }
  }

  def getAsServedSet(id: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getAsServedSetWithPaths(id).right.map(resolveUrls))
    }
  }

  def exportAsServedSet(id: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getPortableAsServedSet(id))
    }
  }

  def importAsServedSet() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[PortableAsServedSet]) {
    request =>
      Future {

        val set = request.body

        val sourceImages = set.images.map {
          img =>
            NewSourceImageRecord(img.sourcePath, img.sourceThumbnailPath, img.sourceKeywords, request.subject.get.identifier.split('#')(0))
        }

        val result = for (
          sourceIds <- imageDatabase.createSourceImageRecords(sourceImages).right;
          mainImageIds <- {
            val mainImages = set.images.zip(sourceIds).map {
              case (img, sourceId) =>
                ProcessedImageRecord(img.mainImagePath, sourceId, ProcessedImagePurpose.AsServedMainImage)
            }

            imageDatabase.createProcessedImageRecords(mainImages).right
          };
          thumbnailIds <- {
            val thumbnailImages = set.images.zip(sourceIds).map {
              case (img, sourceId) =>
                ProcessedImageRecord(img.thumbnailPath, sourceId, ProcessedImagePurpose.AsServedThumbnail)
            }

            imageDatabase.createProcessedImageRecords(thumbnailImages).right
          };
          selectionImageId <- {
            val sourceImageId = set.images.zip(sourceIds).find(_._1.sourcePath == set.selectionSourcePath) match {
              case Some((_, id)) => id
              case None => throw new RuntimeException("Selection image source path must be one of the as served images")
            }

            imageDatabase.createProcessedImageRecords(Seq(ProcessedImageRecord(set.selectionImagePath, sourceImageId, ProcessedImagePurpose.PortionSizeSelectionImage))).right.map(_.head).right
          };
          _ <- {
            val images = set.images.zip(mainImageIds).zip(thumbnailIds).map {
              case ((img, mainImageId), thumbnailId) =>
                NewAsServedImageRecord(mainImageId, thumbnailId, img.weight)
            }

            service.createAsServedSets(Seq(NewAsServedSetRecord(set.id, set.description, selectionImageId, images))).right
          }

        ) yield ()

        translateDatabaseResult(result)

      }
  }

  private def processImages(setId: String, sourceImages: Seq[Long]): Either[ImageServiceOrDatabaseError, (Seq[AsServedImageDescriptor], ImageDescriptor)] = {
    val ssiSourceId = sourceImages(sourceImages.length / 2)

    for (
      imageDescriptors <- imageAdmin.processForAsServed(setId, sourceImages).right;
      ssiDescriptor <- imageAdmin.processForSelectionScreen(ImageAdminService.ssiPrefixAsServed(setId), ssiSourceId).right
    ) yield (imageDescriptors, ssiDescriptor)
  }

  private def createAsServedSetImpl(newSet: NewAsServedSet): Either[ImageServiceOrDatabaseError, AsServedSetWithPaths] =
    for (
      descriptors <- processImages(newSet.id, newSet.images.map(_.sourceImageId)).right;
      _ <- {
        val images = newSet.images.zip(descriptors._1).map {
          case (image, descriptor) => NewAsServedImageRecord(descriptor.mainImage.id, descriptor.thumbnail.id, image.weight)
        }

        service.createAsServedSets(Seq(NewAsServedSetRecord(newSet.id, newSet.description, descriptors._2.id, images))).wrapped.right
      };
      res <- service.getAsServedSetWithPaths(newSet.id).wrapped.right
    ) yield res

  def createAsServedSetFromSource() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[NewAsServedSet]) {
    request =>
      Future {
        val newSet = request.body

        if (newSet.images.isEmpty)
          BadRequest("""{"cause":"As served set must contain at least one image"}""")
        else
          translateImageServiceAndDatabaseResult(createAsServedSetImpl(newSet))
      }
  }

  def createAsServedSet() = deadbolt.restrictToRoles(Roles.superuser)(parse.multipartFormData) {
    request =>
      Future {

        if (!request.body.dataParts.contains("id"))
          BadRequest("""{"cause":"id field missing"}""")
        else if (!request.body.dataParts.contains("weight"))
          BadRequest("""{"cause":"weight field missing"}""")
        else if (request.body.dataParts("weight").exists {
          w =>
            try {
              w.toDouble
              false
            } catch {
              case e: NumberFormatException => true
            }
        })
          BadRequest("""{"cause":"one of the weight fields is not a valid number"}""")
        else if (!request.body.dataParts.contains("description"))
          BadRequest("""{"cause":"description field missing"}""")
        else {
          val files = request.body.files

          val weights = request.body.dataParts("weight").map(_.toDouble)

          if (files.length != weights.length)
            BadRequest("""{"cause":"the number of files must correspond to the number of weight values"}""")
          else {
            val records = files.zip(weights)

            val uploaderName = request.subject.get.identifier.split('#')(0)
            val keywords = request.body.dataParts.getOrElse("keywords", Seq())
            val setId = request.body.dataParts("id").head
            val description = request.body.dataParts("description").head

            val result = for (
              sourceIds <- sequence(records.map {
                record => imageAdmin.uploadSourceImage(ImageAdminService.getSourcePathForAsServed(setId, record._1.filename), Paths.get(record._1.ref.file.getPath), keywords, uploaderName)
              }).right;
              result <- createAsServedSetImpl(NewAsServedSet(setId, description, sourceIds.zip(weights).map { case (id, weight) => NewAsServedImage(id, weight) })).right)
              yield result

            translateImageServiceAndDatabaseResult(result)
          }
        }
      }
  }

  private def cleanUpOldImages(set: AsServedSetRecord): Either[Nothing, Unit] = {
    val images = List(set.selectionImageId) ++ set.images.map(_.mainImageId) ++ set.images.map(_.thumbnailId)

    // It's OK to continue if image deletion failed, but still log it
    imageAdmin.deleteProcessedImages(images) match {
      case Right(()) => Right(())
      case Left(e) =>
        Logger.warn("Could not delete old as served images", e.exception)
        Right(())
    }
  }

  def updateAsServedSet(id: String) = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[NewAsServedSet]) {
    request =>
      Future {
        val update = request.body

        val result = for (
          oldSet <- service.getAsServedSetRecord(id).wrapped.right;
          newDescriptors <- processImages(update.id, update.images.map(_.sourceImageId)).right;
          _ <- {
            val newImageRecords = newDescriptors._1.zip(update.images.map(_.weight)).map {
              case (AsServedImageDescriptor(ImageDescriptor(mainImageId, _), ImageDescriptor(thumbnailId, _)), weight) =>
                NewAsServedImageRecord(mainImageId, thumbnailId, weight)
            }

            service.updateAsServedSet(id, NewAsServedSetRecord(update.id, update.description, newDescriptors._2.id, newImageRecords)).wrapped.right
          };
          _ <- cleanUpOldImages(oldSet).right;
          res <- service.getAsServedSetWithPaths(update.id).wrapped.right
        ) yield res

        translateImageServiceAndDatabaseResult(result)
      }
  }
}
