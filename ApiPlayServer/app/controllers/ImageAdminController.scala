package controllers

import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import org.slf4j.LoggerFactory
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{ImageAdminService, ImageDatabaseService, ImageServiceOrDatabaseError, ImageStorageService}
import upickle.default._

import scala.concurrent.Future

case class ResolvedSourceImageRecord(id: Long, fullSizeUrl: String, fixedSizeUrl: String, keywords: Seq[String], uploader: String, uploadedAt: String)

class ImageAdminController @Inject() (service: ImageAdminService, databaseService: ImageDatabaseService, storageService: ImageStorageService, deadbolt: DeadboltActionsAdapter) extends Controller with ImageOrDatabaseServiceErrorHandler {

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

  def listSourceImages(offset: Int) = deadbolt.restrict(Roles.superuser) {
    request =>
      Future {

        val resolvedRecords = databaseService.listSourceImageRecords(offset, 20).right.map {
          _.map {
            record =>
              ResolvedSourceImageRecord(record.id, storageService.getUrl(record.path), storageService.getUrl(record.thumbnailPath), record.keywords, record.uploader, record.uploadedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          }
        }

        translateDatabaseResult(resolvedRecords)
      }
  }

  def filterSourceImages() = deadbolt.restrict(Roles.superuser) {
    request =>
      Future {
        ???
      }
  }
}
