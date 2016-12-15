package controllers

import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import be.objectify.deadbolt.scala.AuthenticatedRequest
import org.slf4j.LoggerFactory
import parsers.UpickleUtil
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{AnyContent, Controller}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import upickle.default._

import scala.concurrent.Future

class ImageAdminController @Inject()(service: ImageAdminService, databaseService: ImageDatabaseService, storageService: ImageStorageService, deadbolt: DeadboltActionsAdapter)
  extends Controller
    with ImageOrDatabaseServiceErrorHandler
    with UpickleUtil {

  private val logger = LoggerFactory.getLogger(classOf[ImageAdminController])

  case class FilterRequest(offset: Int, limit: Int, keywords: Seq[String])

  case class ClientSourceImageRecord(id: Long, fullSizeUrl: String, fixedSizeUrl: String, keywords: Seq[String], uploader: String, uploadedAt: String)

  private def uploadImpl(pathFunc: Option[String => String], request: AuthenticatedRequest[AnyContent]) = Future {
    request.body.asMultipartFormData match {
      case Some(formData) => {

        val uploaderName = request.subject.get.identifier

        val keywords = formData.dataParts.get("keywords").getOrElse(Seq())

        if (formData.files.length < 1)
          BadRequest("""{"cause":"Expected file attachments"}""").as(ContentTypes.JSON)
        else {
          val results = formData.files.map {
            file =>

              val suggestedPath = pathFunc match {
                case Some(f) => f(file.filename)
                case None => file.filename
              }

              service.uploadSourceImage(suggestedPath, Paths.get(file.ref.file.getPath), keywords, uploaderName)
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

  def uploadSourceImage() = deadbolt.restrict(Roles.superuser) {
    request => uploadImpl(None, request)
  }

  def uploadSourceImageForAsServed(setId: String) = deadbolt.restrict(Roles.superuser) {
    request => uploadImpl(Some(originalPath => ImageAdminService.getSourcePathForAsServed(setId, originalPath)), request)
  }

  def uploadSourceImageForImageMap(id: String) = deadbolt.restrict(Roles.superuser) {
    request => uploadImpl(Some(originalPath => ImageAdminService.getSourcePathForImageMap(id, originalPath)), request)
  }

  private def toClientRecords(records: Seq[SourceImageRecord]): Seq[ClientSourceImageRecord] = records.map {
    record =>
      ClientSourceImageRecord(record.id, storageService.getUrl(record.path), storageService.getUrl(record.thumbnailPath), record.keywords, record.uploader, record.uploadedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  }

  def listSourceImages(offset: Int, limit: Int, searchTerm: Option[String]) = deadbolt.restrict(Roles.superuser) {
    request =>
      Future {
        val resolvedRecords = databaseService.listSourceImageRecords(offset, Math.max(Math.min(limit, 100), 0), searchTerm).right.map(toClientRecords)

        translateDatabaseResult(resolvedRecords)
      }
  }

  def updateSourceImage(id: Int) = deadbolt.restrict(Roles.superuser)(upickleBodyParser[SourceImageRecordUpdate]) {
    request =>
      Future {
        translateDatabaseResult(databaseService.updateSourceImageRecord(id, request.body))
      }
  }

  def deleteSourceImages() = deadbolt.restrict(Roles.superuser)(upickleBodyParser[Seq[Long]]) {
    request =>
      Future {
        translateImageServiceAndDatabaseResult(service.deleteSourceImages(request.body))
      }
  }
}
