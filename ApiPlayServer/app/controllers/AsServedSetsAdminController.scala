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

import scala.concurrent.Future

import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.DeadboltActionsAdapter
import security.Roles
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSetsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageWithPaths
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSetWithPaths
import upickle.default._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSetRecord
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import java.io.File
import java.nio.file.Paths

import parsers.Upickle._

import scalaz._
import Scalaz._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.PortableAsServedImage
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.PortableAsServedSet
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDatabaseService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.SourceImageRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ProcessedImagePurpose
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ProcessedImageRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageWithUrl
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor
import play.api.mvc.Result
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageServiceError
import uk.ac.ncl.openlab.intake24.services.fooddb.images.AsServedImageDescriptor

case class AsServedImageWithUrls(sourceId: Long, imageUrl: String, thumbnailUrl: String, weight: Double)

case class AsServedSetWithUrls(id: String, description: String, images: Seq[AsServedImageWithUrls])

case class NewAsServedImage(sourceImageId: Long, weight: Double)

case class ExistingAsServedImage(processedImageId: Long, processedThumbnailId: Long, weight: Double)

case class NewAsServedSet(id: String, description: String, images: Seq[NewAsServedImage])

class AsServedSetsAdminController @Inject() (
  service: AsServedSetsAdminService,
  imageDatabase: ImageDatabaseService,
  imageAdmin: ImageAdminService,
  imageStorage: ImageStorageService,
  deadbolt: DeadboltActionsAdapter) extends Controller
    with FoodDatabaseErrorHandler
    with ImageServiceErrorHandler {

  def resolveUrls(image: AsServedImageWithPaths): AsServedImageWithUrls =
    AsServedImageWithUrls(image.sourceId, imageStorage.getUrl(image.imagePath), imageStorage.getUrl(image.thumbnailPath), image.weight)

  def resolveUrls(set: AsServedSetWithPaths): AsServedSetWithUrls = AsServedSetWithUrls(set.id, set.description, set.images.map(resolveUrls))

  def listAsServedSets() = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.listAsServedSets())
    }
  }

  def getAsServedSet(id: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getAsServedSet(id).right.map(resolveUrls))
    }
  }

  def exportAsServedSet(id: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getPortableAsServedSet(id))
    }
  }

  def importAsServedSet() = deadbolt.restrict(Roles.superuser)(upickleRead[PortableAsServedSet]) {
    request =>
      Future {

        val set = request.body

        val sourceImages = set.images.map {
          img =>
            SourceImageRecord(img.sourcePath, img.sourceKeywords, request.subject.get.identifier.split('#')(0))
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

            imageDatabase.createProcessedImageRecords(Seq(ProcessedImageRecord(set.selectionImagePath, sourceImageId, ProcessedImagePurpose.PortionSizeSelectionImage))).right.map(_(0)).right
          };
          _ <- {
            val images = set.images.zip(mainImageIds).zip(thumbnailIds).map {
              case ((img, mainImageId), thumbnailId) =>
                AsServedImageRecord(mainImageId, thumbnailId, img.weight)
            }

            service.createAsServedSets(Seq(AsServedSetRecord(set.id, set.description, selectionImageId, images))).right
          }

        ) yield ()

        translateResult(result)

      }
  }

  private def processImages(setId: String, sourceImages: Seq[Long]): Either[ImageServiceError, (Seq[AsServedImageDescriptor], ImageDescriptor)] = {
    val ssiSourceId = sourceImages(sourceImages.length / 2)

    for (
      imageDescriptors <- imageAdmin.processForAsServed(setId, sourceImages).right;
      ssiDescriptor <- imageAdmin.processForSelectionScreen(ImageAdminService.ssiPrefixAsServed(setId), ssiSourceId).right
    ) yield (imageDescriptors, ssiDescriptor)
  }

  private def createAsServedSetImpl(newSet: NewAsServedSet): Result = {
    processImages(newSet.id, newSet.images.map(_.sourceImageId)) match {
      case Right((imageDescriptors, ssiDescriptor)) => {
        val images = newSet.images.zip(imageDescriptors).map {
          case (image, descriptor) => AsServedImageRecord(descriptor.mainImage.id, descriptor.thumbnail.id, image.weight)
        }

        val result = for (
          _ <- service.createAsServedSets(Seq(AsServedSetRecord(newSet.id, newSet.description, ssiDescriptor.id, images))).right;
          res <- service.getAsServedSet(newSet.id).right
        ) yield res

        translateResult(result)
      }
      case Left(error) => translateError(error)
    }
  }

  def createAsServedSetFromSource() = deadbolt.restrict(Roles.superuser)(upickleRead[NewAsServedSet]) {
    request =>
      Future {
        val newSet = request.body

        if (newSet.images.isEmpty)
          BadRequest("""{"cause":"As served set must contain at least one image"}""")
        else
          createAsServedSetImpl(newSet)
      }
  }

  def createAsServedSet() = deadbolt.restrict(Roles.superuser)(parse.multipartFormData) {
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
            val setId = request.body.dataParts("id")(0)
            val description = request.body.dataParts("description")(0)

            val sourceIds = records.map {
              record =>
                imageAdmin.uploadSourceImage(ImageAdminService.getSourcePathForAsServed(setId, record._1.filename), Paths.get(record._1.ref.file.getPath), keywords, uploaderName)
            }.toList.sequenceU

            sourceIds match {
              case Left(e) => translateError(e)
              case Right(ids) => createAsServedSetImpl(NewAsServedSet(setId, description, ids.zip(weights).map { case (id, weight) => NewAsServedImage(id, weight) }))
            }
          }
        }
      }
  }

  def updateAsServedSet(id: String) = deadbolt.restrict(Roles.superuser)(upickleRead[NewAsServedSet]) {
    request =>
      Future {
        val update = request.body
        
        for (
            oldSet <- service.getAsServedSet(id)

       
      }
  }
}
