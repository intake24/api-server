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

package uk.ac.ncl.openlab.intake24.services.dataexport.controllers

import java.io.{File, FileWriter}
import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCode
import com.opencsv.CSVWriter
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.http.ContentTypes
import play.api.libs.ws.WSClient
import play.api.mvc.{BaseController, ControllerComponents}
import uk.ac.ncl.openlab.intake24.errors.ErrorUtils
import uk.ac.ncl.openlab.intake24.play.utils.{DatabaseErrorHandler, JsonBodyParser}
import uk.ac.ncl.openlab.intake24.security.authorization.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.dataexport._
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class UserDataExportHandle(requestId: UUID)

sealed trait UserDataExportStatus

object UserDataExportStatus {

  case object Pending extends UserDataExportStatus

  case class Successful(downloadUrl: String) extends UserDataExportStatus

  case class Failed(errorMessage: String) extends UserDataExportStatus

}

case class RespondentWithAuthUrl(id: Long, userName: String, authUrl: String)

case class RespondentWithAuthAndShortUrls(id: Long, userName: String, authUrl: String, shortUrl: String)

class UserDataExportController @Inject()(configuration: Configuration,
                                         service: UserAdminService,
                                         secureUrlService: SecureUrlService,
                                         syncCacheApi: SyncCacheApi,
                                         ws: WSClient,
                                         rab: Intake24RestrictedActionBuilder,
                                         jsonBodyParser: JsonBodyParser,
                                         actorSystem: ActorSystem,
                                         val controllerComponents: ControllerComponents,
                                         implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler {

  val logger = LoggerFactory.getLogger(classOf[UserDataExportController])

  val surveyFrontendUrl = configuration.get[String]("intake24.surveyFrontendUrl")
  val shortUrlServiceUrl = configuration.get[String]("intake24.shortUrlServiceUrl")


  private def shortenUrls(fullUrls: Seq[String]): Try[Seq[String]] = {
    val request = ws.url(shortUrlServiceUrl + "/shorten")
      .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
      .post(fullUrls.asJson.noSpaces)
      .flatMap {
        response =>

          if (StatusCode.int2StatusCode(response.status).isSuccess()) {
            decode[Seq[String]](response.body) match {
              case Left(e) => Future.failed(e)
              case Right(urls) => Future.successful(urls)
            }
          } else {
            Future.failed(new RuntimeException("Short url service returned " + response.status))
          }
      }

    Try {
      Await.ready(request, 10.seconds)
    }.flatMap {
      _.value.get
    }
  }

  private def appendShortUrls(respondents: Seq[RespondentWithAuthUrl]): Try[Seq[RespondentWithAuthAndShortUrls]] = {
    shortenUrls(respondents.map(_.authUrl)).map {
      shortUrls =>
        respondents.zip(shortUrls).map {
          case (r, shortUrl) => RespondentWithAuthAndShortUrls(r.id, r.userName, r.authUrl, shortUrl)
        }
    }
  }


  private def exportImpl(surveyId: String): Try[File] = {

    def getRespondentRecords(offset: Int, limit: Int): Try[Seq[RespondentWithAuthUrl]] =
      ErrorUtils.asTry(for (users <- service.listUsersByRole(Roles.surveyRespondent(surveyId), offset, limit);
                            aliases <- service.getSurveyUserAliases(users.map(_.id), surveyId)) yield {
        users.filter(u => aliases.contains(u.id)).map {
          user =>
            RespondentWithAuthUrl(user.id, aliases(user.id).userName, surveyFrontendUrl + s"/surveys/$surveyId?auth=${aliases(user.id).urlAuthToken}")
        }
      })


    def writeRows(respondents: Seq[RespondentWithAuthAndShortUrls], writer: CSVWriter): Try[Unit] = Try {
      respondents.foreach {
        row =>
          writer.writeNext(Array(row.id.toString, row.userName, row.authUrl, row.shortUrl))
      }
    }

    def exportRemaining(offset: Int, batchSize: Int, writer: CSVWriter): Try[Unit] =
      getRespondentRecords(offset, batchSize).flatMap {
        respondents =>
          if (respondents.isEmpty)
            Success(())
          else for (withShortUrls <- appendShortUrls(respondents);
                    _ <- writeRows(withShortUrls, writer);
                    _ <- exportRemaining(offset + respondents.length, batchSize, writer)) yield ()
      }

    Try {
      val file = File.createTempFile("intake24", ".csv")
      val writer = new CSVWriter(new FileWriter(file))

      (file, writer)
    }.flatMap {
      case (file, writer) =>
        writer.writeNext(Array("Intake24 user ID", "Survey user ID", "Authentication URL", "Short authentication URL"))
        val result = exportRemaining(0, 1000, writer).map(_ => file)
        Try {
          writer.close()
        }
        result
    }
  }

  def cacheKey(surveyId: String, requestId: String): String = s"intake24.userDataExport.$requestId"

  def exportRespondentAuthUrlsAsCsv(surveyId: String) = rab.restrictToRoles(Roles.surveyStaff(surveyId)) {
    Future {

      val requestId = UUID.randomUUID().toString

      syncCacheApi.set(cacheKey(surveyId, requestId), (UserDataExportStatus.Pending:UserDataExportStatus).asJson.noSpaces, 10 minutes)

      actorSystem.scheduler.scheduleOnce(0.seconds) {
        val result: UserDataExportStatus = exportImpl(surveyId).flatMap {
          file =>
            val dateStamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(Clock.systemUTC().instant(), ZoneId.systemDefault).withNano(0)).replace(":", "").replace("T", "")
            secureUrlService.createUrl(s"intake24-$surveyId-auth-urls-$dateStamp.csv", file, ZonedDateTime.now().plusMinutes(10))
        } match {
          case Success(url) =>
            UserDataExportStatus.Successful(url.toString)
          case Failure(exception) =>
            logger.error("Could not export file", exception)
            UserDataExportStatus.Failed(s"${exception.getClass}: ${exception.getMessage}")
        }

        syncCacheApi.set(cacheKey(surveyId, requestId), result.asJson.noSpaces, 10 minutes)
      }

      Ok(requestId.asJson.noSpaces).as(ContentTypes.JSON)
    }
  }


  def getUrlExportStatus(surveyId: String, requestId: String) = rab.restrictToRoles(Roles.surveyStaff(surveyId)) {
    Future {
      syncCacheApi.get[String](cacheKey(surveyId, requestId)) match {
        case Some(value) => Ok(value).as(ContentTypes.JSON)
        case None => NotFound
      }
    }
  }
}
