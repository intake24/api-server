package errorhandler

import javax.inject._

import io.circe.generic.auto._
import parsers.JsonUtils
import play.api._
import play.api.http.{ContentTypes, DefaultHttpErrorHandler}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import uk.ac.ncl.openlab.intake24.api.data.ErrorDescription

import scala.concurrent._

@Singleton
class Intake24ErrorHandler @Inject()(
                                      env: Environment,
                                      config: Configuration,
                                      sourceMapper: OptionalSourceMapper,
                                      router: Provider[Router]
                                    ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with JsonUtils {

  override def onBadRequest(request: RequestHeader, message: String): Future[Result] =
    Future.successful {
      BadRequest(toJsonString(ErrorDescription("BadRequest", message))).as(ContentTypes.JSON)
    }

}