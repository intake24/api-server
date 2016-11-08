package controllers

import uk.ac.ncl.openlab.intake24.services.fooddb.images.{ImageAdminService, ImageServiceOrDatabaseError}
import javax.inject.Inject

import security.Roles
import security.DeadboltActionsAdapter

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import org.slf4j.LoggerFactory
import play.api.Logger
import play.api.http.ContentTypes
import java.nio.file.Paths

import upickle.default._

import scalaz._
import Scalaz._

class ImageAdminController @Inject() (service: ImageAdminService, deadbolt: DeadboltActionsAdapter) extends Controller with ImageOrDatabaseServiceErrorHandler {

  private val logger = LoggerFactory.getLogger(classOf[ImageAdminController])

  def uploadSourceImage() = deadbolt.restrict(Roles.superuser) {
    request =>
      Future {
        request.body.asMultipartFormData match {
          case Some(formData) => {

            val uploaderName = request.subject.get.identifier

            val keywords = formData.dataParts.get("keywords").getOrElse(Seq())

            if (formData.files.length < 1)
              BadRequest("""{"cause":"Expected file attachments"}""").as(ContentTypes.JSON)
            else {
              val results = formData.files.map {
                file =>
                  service.uploadSourceImage(file.filename, Paths.get(file.ref.file.getPath), keywords, uploaderName)
              }.toList

              val error = results.find(_.isLeft).map(_.asInstanceOf[Either[ImageServiceOrDatabaseError, Long]])

              error match {
                case Some(e) => translateImageServiceAndDatabaseResult(e)
                case _ => Ok(write(results.map(_.right.get)))
              }
            }
          }

          case None => BadRequest("""{"cause":"Failed to parse form data"}""").as(ContentTypes.JSON)
        }
      }
  }
}
