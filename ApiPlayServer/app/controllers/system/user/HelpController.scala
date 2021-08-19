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

package controllers.system.user

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import parsers.{JsonBodyParser, JsonUtils}
import play.api.Configuration
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import play.cache.SyncCacheApi
import security._
import sms.SMSService
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService

import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


case class CallbackRequest(name: String, phone: String)

case class FeedbackMessage(like: Boolean, pageUrl: String, body: String)

class HelpController @Inject()(cache: SyncCacheApi,
                               config: Configuration,
                               system: ActorSystem,
                               mailer: MailerClient,
                               smsService: SMSService,
                               userAdminService: UserAdminService,
                               passwordHasherRegistry: PasswordHasherRegistry,
                               rab: Intake24RestrictedActionBuilder,
                               jsonBodyParser: JsonBodyParser,
                               val controllerComponents: ControllerComponents,
                               implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler with JsonUtils {

  val callbackRequestRate = config.get[Int]("intake24.help.callbackRequestRateSeconds")
  val supportEmail = config.get[String]("intake24.supportEmail")
  val feedbackEmail = config.get[String]("intake24.feedbackEmail")

  val logger = LoggerFactory.getLogger(classOf[HelpController])

  private def getCacheKey(subject: Intake24AccessToken) = {
    s"reject-callback-${subject.userId.toString}"
  }

  // TODO: captcha to prevent new spam
  // TODO: localise e-mail messages
  def requestCallback(surveyId: String) = rab.restrictToRoles(Roles.surveyRespondent(surveyId))(jsonBodyParser.parse[CallbackRequest]) {
    request =>
      Future {
        val subject = request.subject

        val cacheKey = getCacheKey(subject)

        val userName = subject.jwt.loginInfo.providerID match {
          case SurveyAliasProvider.ID => SurveyAliasUtils.fromString(subject.jwt.loginInfo.providerKey).userName
          case _ => subject.userId.toString
        }

        if (cache.get[String](cacheKey) != null)
          TooManyRequests
        else {
          cache.set(cacheKey, "t", callbackRequestRate)

          val supportUsers = userAdminService.listUsersByRole(Roles.surveySupport(surveyId), 0, 100).right.flatMap {
            users =>
              if (users.isEmpty) {
                logger.warn(s"Support user list is empty for survey $surveyId -- falling back to global support users")
                userAdminService.listUsersByRole(Roles.globalSupport, 0, 100)
              }
              else
                Right(users)
          }

          val result = supportUsers.right.map {
            users =>
              val emailAddresses = users.filter(_.emailNotifications).flatMap(_.email)
              val phoneNumbers = users.filter(_.smsNotifications).flatMap(_.phone)

              if (emailAddresses.isEmpty)
                logger.error(s"No support e-mail addresses are available for survey $surveyId: support user list is empty, none of support users have an e-mail address set, or all available support users have e-mail notifications disabled")
              else
                system.scheduler.scheduleOnce(0.seconds) {
                  try {
                    val message = Email(
                      subject = s"Someone needs help completing their Intake24 survey ($surveyId)",
                      bodyText = Some(s"Please call ${request.body.name} on ${request.body.phone} (survey ID: $surveyId, user ID: $userName)"),
                      from = s"Intake24 <$supportEmail>",
                      to = emailAddresses
                    )

                    mailer.send(message)
                  } catch {
                    case e: Throwable => logger.error("Failed to send e-mail message", e)
                  }
                }

              if (phoneNumbers.isEmpty)
                logger.warn(s"No support phone numbers are available for survey $surveyId: support user list is empty, none of support users have a phone number set, or all available support users have SMS notifiactions disabled")
              else
                system.scheduler.scheduleOnce(0.seconds) {
                  phoneNumbers.foreach {
                    toNumber =>
                      try {
                        smsService.sendMessage(s"Intake24: please call ${request.body.name} on ${request.body.phone} (survey ID: $surveyId, user ID: $userName)", toNumber)
                      } catch {
                        case e: Throwable => logger.error("Failed to send SMS message", e)
                      }
                  }
                }

              ()
          }

          translateDatabaseResult(result)
        }
      }
  }

  def feedback() = rab.restrictToAuthenticated(jsonBodyParser.parse[FeedbackMessage]) {
    request =>
      Future {
        val subject = request.subject

        val cacheKey = getCacheKey(subject)

        val userName = subject.jwt.loginInfo.providerID match {
          case SurveyAliasProvider.ID => SurveyAliasUtils.fromString(subject.jwt.loginInfo.providerKey).userName
          case _ => subject.userId.toString
        }

        if (cache.get[String](cacheKey) != null)
          TooManyRequests
        else {
          cache.set(cacheKey, "t", callbackRequestRate)

          val result = system.scheduler.scheduleOnce(0.seconds) {
            try {
              val message = Email(
                subject = s"Page feedback. ${if (request.body.like) "Liked" else "Disliked"}",
                bodyText = Some(
                  s"""
                     |User: ${userName} \n
                     |Url: ${request.body.pageUrl} \n
                     |Experience: ${if (request.body.like) "Liked" else "Disliked"} \n
                     |${request.body.body}
                  """.stripMargin),
                from = s"Intake24 Feedback <$supportEmail>",
                to = Seq(s"Intake24 Feedback <$feedbackEmail>")
              )

              mailer.send(message)
            } catch {
              case e: Throwable => logger.error("Failed to send e-mail message", e)
            }
          }

          Ok
        }
      }
  }

}
