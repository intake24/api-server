package uk.ac.ncl.openlab.intake24.services.shorturl

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent.ExecutionContext

class RequestHandler @Inject()(errorHandler: HttpErrorHandler,
                               config: Configuration,
                               httpConfig: HttpConfiguration,
                               shortUrlCache: ShortUrlService,
                               filters: HttpFilters,
                               actionBuilder: DefaultActionBuilder,
                               router: Router,
                               implicit val executionContext: ExecutionContext) extends DefaultHttpRequestHandler(
  router, errorHandler, httpConfig, filters
) {

  private val shortUrlDomain = config.get[String]("intake24.urlShort.internal.domain")
  private val logger = LoggerFactory.getLogger(classOf[RequestHandler])

  override def routeRequest(request: RequestHeader) = {
    request.host match {
      case `shortUrlDomain` =>

        logger.debug(s"Resolve request: ${request.host + request.uri}")

        Some(actionBuilder.async {
          shortUrlCache.resolve(request.host + request.uri).map {
            case Some(url) =>
              logger.debug(s"Resolve result: $url")
              Results.PermanentRedirect(url)
            case None =>
              logger.debug(s"Resolve result: not found")
              Results.NotFound
          }
        })

      case _ =>
        super.routeRequest(request)
    }
  }
}