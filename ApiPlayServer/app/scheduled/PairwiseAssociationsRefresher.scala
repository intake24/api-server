package scheduled

import akka.actor.ActorSystem
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Created by Tim Osadchiy on 12/10/2017.
 */
trait PairwiseAssociationsRefresher

@Singleton
class PairwiseAssociationsRefresherImpl @Inject()(settings: PairwiseAssociationsServiceConfiguration,
                                                  system: ActorSystem,
                                                  paService: PairwiseAssociationsService,
                                                  @Named("intake24") implicit val executionContext: ExecutionContext)
  extends PairwiseAssociationsRefresher {

  system.scheduler.scheduleOnce(0.minutes) {
    paService.refresh()
  }

  system.scheduler.scheduleWithFixedDelay(settings.nextRefreshIn, 24.hours)(new Runnable {
    override def run() = paService.refresh()
  })
}
