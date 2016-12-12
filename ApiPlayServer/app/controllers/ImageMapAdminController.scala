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

import parsers.Upickle._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Controller, Result}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import upickle.default._

import scala.concurrent.Future
import scalaz.Scalaz._

import play.api.Logger

class ImageMapAdminController @Inject()(
                                             service: AsServedSetsAdminService,
                                             imageDatabase: ImageDatabaseService,
                                             imageAdmin: ImageAdminService,
                                             imageStorage: ImageStorageService,
                                             deadbolt: DeadboltActionsAdapter) extends Controller
  with ImageOrDatabaseServiceErrorHandler {

  import ImageAdminService.{WrapDatabaseError, WrapImageServiceError}

  def resolveUrls(image: AsServedImageWithPaths): AsServedImageWithUrls =
    AsServedImageWithUrls(image.sourceId, imageStorage.getUrl(image.imagePath), imageStorage.getUrl(image.thumbnailPath), image.weight)

  def resolveUrls(set: AsServedSetWithPaths): AsServedSetWithUrls = AsServedSetWithUrls(set.id, set.description, set.images.map(resolveUrls))

  def createImageMapFromSVG() = deadbolt.restrict(Roles.superuser)(parse.multipartFormData) {
    request =>
      Future {
        if (!request.body.dataParts.contains("baseImage"))
          BadRequest("""{"cause":"baseImage field missing"}""")
        else if (!request.body.dataParts.contains("svgImageMap"))
          BadRequest("""{"cause":"svgImageMap field missing"}""")
        else {

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
            val setId = request.body.dataParts("id").head
            val description = request.body.dataParts("description").head

            val result = for (
              sourceIds <- records.map {
                record => imageAdmin.uploadSourceImage(ImageAdminService.getSourcePathForAsServed(setId, record._1.filename), Paths.get(record._1.ref.file.getPath), keywords, uploaderName)
              }.toList.sequenceU.right;
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

  def updateAsServedSet(id: String) = deadbolt.restrict(Roles.superuser)(upickleRead[NewAsServedSet]) {
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
