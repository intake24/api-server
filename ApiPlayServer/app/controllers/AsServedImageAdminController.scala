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
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageWithPaths
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSetWithPaths
import upickle.default._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImage
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSet
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import java.io.File
import java.nio.file.Paths

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

case class AsServedImageWithUrls(image: ImageWithUrl, thumbnail: ImageWithUrl, weight: Double)

case class AsServedSetWithUrls(id: String, description: String, selectionImage: ImageWithUrl, images: Seq[AsServedImageWithUrls])

case class NewAsServedImage(sourceImageId: Long, weight: Double)

case class NewAsServedSet(id: String, description: String, images: Seq[NewAsServedImage])

class AsServedImageAdminController @Inject() (
  service: AsServedImageAdminService,
  imageDatabase: ImageDatabaseService,
  imageAdmin: ImageAdminService,
  imageStorage: ImageStorageService,
  deadbolt: DeadboltActionsAdapter) extends Controller
    with PickleErrorHandler
    with FoodDatabaseErrorHandler
    with ImageServiceErrorHandler {
  
  def resolveUrl(image: ImageDescriptor) = ImageWithUrl(image.id, imageStorage.getUrl(image.path))

  def resolveUrls(image: AsServedImageWithPaths): AsServedImageWithUrls =
    AsServedImageWithUrls(resolveUrl(image.image), resolveUrl(image.thumbnail), image.weight)

  def resolveUrls(set: AsServedSetWithPaths): AsServedSetWithUrls = AsServedSetWithUrls(set.id, set.description, resolveUrl(set.selectionImage), set.images.map(resolveUrls))

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

  def importAsServedSet() = deadbolt.restrict(Roles.superuser)(parse.tolerantText) {
    request =>
      Future {
        tryWithPickle {
          val set = read[PortableAsServedSet](request.body)

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
                case None  => throw new RuntimeException("Selection image source path must be one of the as served images")
              }
              
              imageDatabase.createProcessedImageRecords(Seq(ProcessedImageRecord(set.selectionImagePath, sourceImageId, ProcessedImagePurpose.PortionSizeSelectionImage))).right.map(_(0)).right
            };
            _ <- {
              val images = set.images.zip(mainImageIds).zip(thumbnailIds).map {
                case ((img, mainImageId), thumbnailId) =>
                  AsServedImage(mainImageId, thumbnailId, img.weight)
              }

              service.createAsServedSets(Seq(AsServedSet(set.id, set.description, selectionImageId, images))).right
            }

          ) yield ()

          translateResult(result)
        }
      }
  }

  def createAsServedSetFromSource() = deadbolt.restrict(Roles.superuser)(parse.tolerantText) {
    request =>
      Future {
        tryWithPickle {
          val newSet = read[NewAsServedSet](request.body)

          imageAdmin.processForAsServed(newSet.id, newSet.images.map(_.sourceImageId)) match {
            case Right(descriptors) => {
              val images = newSet.images.zip(descriptors.images).map {
                case (image, descriptor) => AsServedImage(descriptor.mainImage.id, descriptor.thumbnail.id, image.weight)
              }

              translateResult(service.createAsServedSets(Seq(AsServedSet(newSet.id, newSet.description, descriptors.selectionImage.id, images))))
            }
            case Left(error) => translateError(error)
          }
        }
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
              case Right(ids) => {
                imageAdmin.processForAsServed(setId, ids) match {
                  case Right(descriptors) => {
                    val images = records.zip(descriptors.images).map {
                      case ((_, weight), descriptor) => AsServedImage(descriptor.mainImage.id, descriptor.thumbnail.id, weight)
                    }

                    translateResult(service.createAsServedSets(Seq(AsServedSet(setId, description, descriptors.selectionImage.id, images))))
                  }
                  case Left(e) => translateError(e)
                }
              }
            }
          }
        }
      }
  }
}
