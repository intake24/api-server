package scheduled

import javax.inject.{Inject, Singleton}

import scala.concurrent.duration._
import akka.actor.ActorSystem
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration}

import scala.concurrent.ExecutionContext

/**
  * Created by Tim Osadchiy on 12/10/2017.
  */
trait PairwiseAssociationsRefresher

@Singleton
class PairwiseAssociationsRefresherImpl @Inject()(settings: PairwiseAssociationsServiceConfiguration,
                                                  system: ActorSystem,
                                                  paService: PairwiseAssociationsService,
                                                  implicit val executionContext: ExecutionContext)
  extends PairwiseAssociationsRefresher {

  paService.refresh()

  system.scheduler.schedule(settings.nextRefreshIn, 24.hours) {
    paService.refresh()
  }

}
