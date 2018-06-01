package urlShort

import javax.inject.Inject

import play.api.Configuration
import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent.ExecutionContext

class RequestHandler @Inject()(errorHandler: HttpErrorHandler,
                               config: Configuration,
                               httpConfig: HttpConfiguration,
                               shortUrlCache: ShortUrlCache,
                               filters: HttpFilters,
                               actionBuilder: DefaultActionBuilder,
                               router: Router,
                               implicit val executionContext: ExecutionContext) extends DefaultHttpRequestHandler(
  router, errorHandler, httpConfig, filters
) {

  private val shortUrlDomain = config.get[String]("intake24.urlShort.internal.domain")

  override def routeRequest(request: RequestHeader) = {
    request.host match {
      case `shortUrlDomain` =>
        Some(actionBuilder.async {
          shortUrlCache.resolve(request.host + request.uri).map {
            case Some(url) => Results.PermanentRedirect(url)
            case None => Results.NotFound
          }
        })

      case _ =>
        super.routeRequest(request)
    }
  }
}