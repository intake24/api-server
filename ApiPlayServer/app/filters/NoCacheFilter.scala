package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class NoCacheFilter @Inject()(implicit val mat: Materializer,
                              implicit val executionContext: ExecutionContext) extends Filter {
  def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    next.apply(rh).map {
      result =>
        result.withHeaders("Cache-Control" -> "no-cache, no-store, must-revalidate")
    }
  }
}