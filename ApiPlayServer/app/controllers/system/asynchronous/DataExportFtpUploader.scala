package controllers.system.asynchronous

import javax.inject.{Inject, Named, Singleton}

import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.mailer.{Email, MailerClient}
import uk.ac.ncl.openlab.intake24.errors.{AnyError, ErrorUtils}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{UserAdminService, UserProfile}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataExportFtpUploader @Inject()(configuration: Configuration,
                                      userAdminService: UserAdminService,
                                      mailerClient: MailerClient,
                                      @Named("intake24") implicit val executionContext: ExecutionContext) {

  val logger = LoggerFactory.getLogger(classOf[DataExportFtpUploader])


}
