package uk.ac.ncl.openlab.intake24.play.utils.filters

import akka.stream.Materializer
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.http.HttpVerbs
import play.api.mvc.{Filter, RequestHeader, Result}
import play.mvc.Http

import scala.concurrent.{ExecutionContext, Future}

class OptionsLog @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  val logger = LoggerFactory.getLogger(classOf[OptionsLog])

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    if (requestHeader.method == Http.HttpVerbs.OPTIONS) {
      logger.debug(s"Request: ${requestHeader.toString()}")
      requestHeader.headers.headers.foreach {
        case (header, value) =>
          logger.debug(s"  $header: $value")
      }
    }

    nextFilter(requestHeader)
  }
}