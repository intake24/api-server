package scheduled

import javax.inject.Named

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


@Singleton
class ScheduledDataExporter @Inject()(config: Configuration,
                                      system: ActorSystem,
                                      @Named("long-tasks") implicit val executionContext: ExecutionContext,
                                     ) extends ErrorDigestSender {


}
