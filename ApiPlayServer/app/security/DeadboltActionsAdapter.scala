package security

import be.objectify.deadbolt.scala.ActionBuilders
import com.google.inject.Inject
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.anyOf
import be.objectify.deadbolt.scala.allOf
import be.objectify.deadbolt.scala.AuthenticatedRequest
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.AnyContent

class DeadboltActionsAdapter @Inject() (actionBuilders: ActionBuilders, handlerCache: HandlerCache) {
  class RestrictActionBuilderAdapter(anyOfRoles: Seq[String]) {

    private val defaultHandler = handlerCache()
    private val roleGroups = anyOfRoles.toList.map(Array(_))
    private val actionBuilder = actionBuilders.RestrictAction.RestrictActionBuilder(roleGroups)

    def apply[A](bodyParser: BodyParser[A])(block: AuthenticatedRequest[A] => Future[Result]): Action[A] = actionBuilder(bodyParser)(block)(defaultHandler)

    def apply(block: AuthenticatedRequest[AnyContent] => Future[Result]): Action[AnyContent] = actionBuilder(block)(defaultHandler)

    def apply(block: => Future[Result]): Action[AnyContent] = actionBuilder(block)(defaultHandler)
  }

  def restrict(anyOfRoles: String*) = new RestrictActionBuilderAdapter(anyOfRoles)
}