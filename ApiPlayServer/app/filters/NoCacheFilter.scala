package filters

import javax.inject.Inject

import akka.stream.Materializer

import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class NoCacheFilter @Inject()(implicit val mat: Materializer) extends Filter {
  def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    next.apply(rh).map {
      result =>
        result.withHeaders("Cache-Control" -> "no-cache, no-store, must-revalidate")
    }
  }
}