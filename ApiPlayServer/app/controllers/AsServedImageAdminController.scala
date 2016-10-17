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

case class AsServedImageWithUrls(mainImageId: Long, mainImageUrl: String, thumbnailId: Long, thumbnailUrl: String, weight: Double)

case class AsServedSetWithUrls(id: String, description: String, images: Seq[AsServedImageWithUrls])

case class NewAsServedImage(sourceImageId: Long, weight: Double)

case class NewAsServedSet(id: String, description: String, images: Seq[NewAsServedImage])

class AsServedImageAdminController @Inject() (
  service: AsServedImageAdminService,
  imageAdmin: ImageAdminService,
  imageStorage: ImageStorageService,
  deadbolt: DeadboltActionsAdapter) extends Controller
    with PickleErrorHandler
    with FoodDatabaseErrorHandler
    with ImageServiceErrorHandler {

  def resolveUrls(image: AsServedImageWithPaths): AsServedImageWithUrls =
    AsServedImageWithUrls(image.mainImageId, imageStorage.getUrl(image.mainImagePath),
      image.thumbnailId, imageStorage.getUrl(image.thumbnailPath), image.weight)

  def resolveUrls(set: AsServedSetWithPaths): AsServedSetWithUrls =
    AsServedSetWithUrls(set.id, set.description, set.images.map(resolveUrls))

  def listAsServedSets() = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.listAsServedSets())
    }
  }

  def getAsServedSet(id: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.getAsServedSet(id).right.map(resolveUrls))
    }
  }

  def createAsServedSet() = deadbolt.restrict(Roles.superuser)(parse.tolerantText) {
    request =>
      Future {
        tryWithPickle {
          val newSet = read[NewAsServedSet](request.body)

          imageAdmin.processForAsServed(newSet.images.map(_.sourceImageId)) match {
            case Right(descriptors) => {
              val images = newSet.images.zip(descriptors).map {
                case (image, descriptor) => AsServedImage(descriptor.mainImage.id, descriptor.thumbnail.id, image.weight)
              }

              translateError(service.createAsServedSets(Seq(AsServedSet(newSet.id, newSet.description, images))))
            }
            case Left(error) => translateError(error)
          }
        }
      }
  }
}
