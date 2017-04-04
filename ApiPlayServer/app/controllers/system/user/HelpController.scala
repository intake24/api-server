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

import javax.inject.Inject

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import controllers.DatabaseErrorHandler
import parsers.JsonUtils
import play.api.{Configuration, Logger}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import play.cache.CacheApi
import security._
import sms.SMSService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService

import scala.concurrent.Future
import scala.concurrent.duration._
import io.circe.generic.auto._
import models.Intake24Subject


case class CallbackRequest(name: String, phone: String)

class HelpController @Inject()(cache: CacheApi,
                               config: Configuration,
                               system: ActorSystem,
                               mailer: MailerClient,
                               smsService: SMSService,
                               userAdminService: UserAdminService,
                               passwordHasherRegistry: PasswordHasherRegistry,
                               deadbolt: DeadboltActionsAdapter) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  val callbackRequestRate = config.getInt("intake24.help.callbackRequestRateSeconds").get

  // TODO: captcha to prevent new spam
  // TODO: localise e-mail messages
  def requestCallback(surveyId: String) = deadbolt.restrictToRoles(Roles.surveyRespondent(surveyId))(jsonBodyParser[CallbackRequest]) {
    request =>
      Future {
        val subject = request.subject.get.asInstanceOf[Intake24Subject]

        val cacheKey = s"reject-callback-${subject.userId.toString}"

        val userName = subject.jwt.loginInfo.providerID match {
          case SurveyAliasProvider.ID => SurveyAliasUtils.fromString(subject.jwt.loginInfo.providerKey).userName
          case _ => subject.userId.toString
        }

        if (cache.get[String](cacheKey) != null)
          TooManyRequests
        else {
          cache.set(cacheKey, "t", callbackRequestRate)

          val supportUsers = userAdminService.getSurveySupportUsers(surveyId).right.flatMap {
            users =>
              if (users.isEmpty) {
                Logger.warn(s"Support user list is empty for survey $surveyId -- falling back to global support users")
                userAdminService.getGlobalSupportUsers()
              }
              else
                Right(users)
          }

          val result = supportUsers.right.map {
            users =>
              val emailAddresses = users.flatMap(_.email)
              val phoneNumbers = users.flatMap(_.phone)

              if (emailAddresses.isEmpty)
                Logger.error(s"No support e-mail addresses are available for survey $surveyId: support user list is empty or none of support users have an e-mail address set")
              else
                system.scheduler.scheduleOnce(0 seconds) {
                  try {
                    val message = Email(
                      subject = s"Someone needs help completing their Intake24 survey ($surveyId)",
                      bodyText = Some(s"Please call ${request.body.name} on ${request.body.phone} (survey ID: $surveyId, user ID: $userName)"),
                      from = "Intake24 <support@intake24.co.uk>",
                      to = emailAddresses
                    )

                    mailer.send(message)
                  } catch {
                    case e: Throwable => Logger.error("Failed to send e-mail message", e)
                  }
                }

              if (phoneNumbers.isEmpty)
                Logger.warn(s"No support phone numbers are available for survey $surveyId: support user list is empty or none of support users have a phone number set")
              else
                system.scheduler.scheduleOnce(0 seconds) {
                  phoneNumbers.foreach {
                    toNumber =>
                      try {
                        smsService.sendMessage(s"Intake24: please call ${request.body.name} on ${request.body.phone} (survey ID: $surveyId, user ID: $userName)", toNumber)
                      } catch {
                        case e: Throwable => Logger.error("Failed to send SMS message", e)
                      }
                  }
                }

              ()
          }

          translateDatabaseResult(result)
        }
      }
  }
}
