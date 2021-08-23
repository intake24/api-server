package scheduled

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.mailer.{Email, MailerClient}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{ClientErrorReport, ClientErrorService}

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import javax.inject.Named
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait ErrorDigestSender

@Singleton
class ErrorDigestSenderImpl @Inject()(config: Configuration,
                                      system: ActorSystem,
                                      errorService: ClientErrorService,
                                      mailer: MailerClient,
                                      @Named("intake24") implicit val executionContext: ExecutionContext,
                                     ) extends ErrorDigestSender {


  val frequency = config.get[Int]("intake24.errorDigest.frequencyMinutes")

  val logger = LoggerFactory.getLogger(classOf[ErrorDigestSenderImpl])

  system.scheduler.schedule(0.minutes, frequency.minutes)(
    new Runnable {
      def formatReport(report: ClientErrorReport) = {
        val sb = new StringBuilder

        sb.append(s"Survey ID: ${report.surveyId.getOrElse("N/A")}\n")
        sb.append(s"User ID: ${report.userId.getOrElse("N/A")}\n\n")
        sb.append(s"Server time: ${DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.ofInstant(report.reportedAt, ZoneId.systemDefault))}\n\n")
        sb.append(s"Stack trace:\n\n")

        report.stackTrace.foreach {
          stackTraceLine =>
            sb.append(stackTraceLine)
            sb.append("\n")
        }

        sb.append(s"Survey state:\n")
        sb.append(s"${report.surveyStateJSON}\n")

        sb.toString
      }

      def buildDigest(newReports: Seq[ClientErrorReport]): String = newReports.map(formatReport).mkString("\n---------\n")

      override def run() = {

        errorService.getNewErrorReports() match {
          case Right(reports) =>
            if (reports.nonEmpty) {
              val subject = s"Intake24 had ${reports.size} client error(s) since last report"

              try {
                val email = Email(subject, "Intake24 <no-reply@intake24.co.uk>", Seq("bugs@intake24.co.uk"), Some(buildDigest(reports)))
                mailer.send(email)

                errorService.markAsSeen(reports.map(_.id)) match {
                  case Left(error) => logger.error("Failed to mark error reports as seen", error.exception)
                  case Right(_) => ()
                }

              } catch {
                case e: Throwable => logger.error("Failed to send error digest e-mail", e)
              }
            } else {
              logger.info("No new errors to report")
            }

          case Left(error) => logger.error("Failed to get new error reports", error.exception)
        }
      }
    })
}
