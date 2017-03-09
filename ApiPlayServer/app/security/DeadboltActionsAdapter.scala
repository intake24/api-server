package security

import be.objectify.deadbolt.scala.{ActionBuilders, AuthenticatedRequest, DeadboltHandler, HandlerKey}
import be.objectify.deadbolt.scala.cache.HandlerCache
import com.google.inject.Inject
import play.api.mvc._

import scala.concurrent.Future

class DeadboltActionsAdapter @Inject()(actionBuilders: ActionBuilders, handlerCache: HandlerCache) {

  class RestrictActionBuilderAdapter(anyOfRoles: Seq[String]) {

    private val roleGroups = anyOfRoles.toList.map(Array(_))
    private val actionBuilder = actionBuilders.RestrictAction.RestrictActionBuilder(roleGroups)

    def apply[A](bodyParser: BodyParser[A])(block: AuthenticatedRequest[A] => Future[Result]): Action[A] = actionBuilder(bodyParser)(block)(handlerCache(AccessHandler))

    def apply(block: AuthenticatedRequest[AnyContent] => Future[Result]): Action[AnyContent] = actionBuilder(block)(handlerCache(AccessHandler))

    def apply(block: => Future[Result]): Action[AnyContent] = actionBuilder(block)(handlerCache(AccessHandler))
  }

  class SubjectPresentActionBuilderAdapter(handlerKey: HandlerKey) {

    private val actionBuilder = actionBuilders.SubjectPresentAction.SubjectPresentActionBuilder()

    def apply(block: AuthenticatedRequest[Unit] => Future[Result]): Action[Unit] =
      actionBuilder(BodyParsers.parse.empty)(block)(handlerCache(handlerKey))
  }

  def restrictRefresh = new SubjectPresentActionBuilderAdapter(RefreshHandler)

  def restrictToRoles(anyOfRoles: String*) = new RestrictActionBuilderAdapter(anyOfRoles)

  def restrictToAuthenticated = new SubjectPresentActionBuilderAdapter(AccessHandler)

}