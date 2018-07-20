package controllers

import java.time.format.DateTimeFormatter
import javax.inject.Inject

import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import parsers.{JsonBodyParser, JsonUtils}
import play.api.http.ContentTypes
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import security.authorization.AuthorizedRequest
import security.{Intake24AccessToken, Intake24RestrictedActionBuilder}
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

import scala.concurrent.{ExecutionContext, Future}

class ImageAdminController @Inject()(service: ImageAdminService,
                                     databaseService: ImageDatabaseService,
                                     storageService: ImageStorageService,
                                     foodAuthChecks: FoodAuthChecks,
                                     rab: Intake24RestrictedActionBuilder,
                                     playBodyParsers: PlayBodyParsers,
                                     jsonBodyParser: JsonBodyParser,
                                     val controllerComponents: ControllerComponents,
                                     implicit val executionContext: ExecutionContext) extends BaseController
  with ImageOrDatabaseServiceErrorHandler
  with JsonUtils {

  private val logger = LoggerFactory.getLogger(classOf[ImageAdminController])

  case class FilterRequest(offset: Int, limit: Int, keywords: Seq[String])

  case class ClientSourceImageRecord(id: Long, fullSizeUrl: String, fixedSizeUrl: String, keywords: Seq[String], uploader: String, uploadedAt: String)

  private def uploadImpl(pathFunc: Option[String => String], request: AuthorizedRequest[MultipartFormData[TemporaryFile], Intake24AccessToken]) = Future {

    val uploaderName = request.subject.userId.toString // FIXME: better uploader string

    val keywords = request.body.dataParts.get("keywords").getOrElse(Seq())

    if (request.body.files.length < 1)
      BadRequest("""{"cause":"Expected file attachments"}""").as(ContentTypes.JSON)
    else {
      val results = request.body.files.map {
        file =>

          val suggestedPath = pathFunc match {
            case Some(f) => f(file.filename)
            case None => file.filename
          }

          service.uploadSourceImage(suggestedPath, file.ref.path, keywords, uploaderName)
      }.toList

      val error = results.find(_.isLeft)

      error match {
        case Some(e) => translateImageServiceAndDatabaseResult(e.right.map(toClientRecord(_)))
        case _ => Ok(toJsonString(toClientRecords(results.map(_.right.get))))
      }
    }
  }


  def uploadSourceImage() = rab.restrictAccess(foodAuthChecks.canUploadSourceImages)(playBodyParsers.multipartFormData) {
    request => uploadImpl(None, request)
  }

  def uploadSourceImageForAsServed(setId: String) = rab.restrictToRoles(Roles.superuser, Roles.foodsAdmin)(playBodyParsers.multipartFormData) {
    request => uploadImpl(Some(originalPath => ImageAdminService.getSourcePathForAsServed(setId, originalPath)), request)
  }

  def uploadSourceImageForImageMap(id: String) = rab.restrictToRoles(Roles.superuser, Roles.foodsAdmin)(playBodyParsers.multipartFormData) {
    request => uploadImpl(Some(originalPath => ImageAdminService.getSourcePathForImageMap(id, originalPath)), request)
  }

  private def toClientRecords(records: Seq[SourceImageRecord]): Seq[ClientSourceImageRecord] = records.map(toClientRecord(_))

  private def toClientRecord(record: SourceImageRecord): ClientSourceImageRecord =
    ClientSourceImageRecord(record.id, storageService.getUrl(record.path), storageService.getUrl(record.thumbnailPath), record.keywords, record.uploader, record.uploadedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

  def listSourceImages(offset: Int, limit: Int, searchTerm: Option[String]) = rab.restrictToRoles(Roles.superuser, Roles.foodsAdmin, Roles.imagesAdmin) {
    request =>
      Future {
        val resolvedRecords = databaseService.listSourceImageRecords(offset, Math.max(Math.min(limit, 100), 0), searchTerm).right.map(toClientRecords)

        translateDatabaseResult(resolvedRecords)
      }
  }

  def updateSourceImage(id: Int) = rab.restrictToRoles(Roles.superuser, Roles.foodsAdmin)(jsonBodyParser.parse[SourceImageRecordUpdate]) {
    request =>
      Future {
        translateDatabaseResult(databaseService.updateSourceImageRecord(id, request.body))
      }
  }

  def deleteSourceImages() = rab.restrictToRoles(Roles.superuser, Roles.foodsAdmin)(jsonBodyParser.parse[Seq[Long]]) {
    request =>
      Future {
        translateImageServiceAndDatabaseResult(service.deleteSourceImages(request.body))
      }
  }

  def processForSelectionScreen(pathPrefix: String, sourceId: Long) = rab.restrictToRoles(Roles.superuser, Roles.foodsAdmin) {
    _ =>
      Future {
        translateImageServiceAndDatabaseResult(service.processForSelectionScreen(pathPrefix, sourceId))
      }
  }
}
