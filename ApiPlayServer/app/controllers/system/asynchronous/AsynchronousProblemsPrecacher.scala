package controllers.system.asynchronous

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

import akka.actor.ActorSystem
import modules.ProblemCheckerService
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.errors.{AnyError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{FoodBrowsingAdminService, LocalesAdminService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


@Singleton
class AsynchronousProblemsPrecacher @Inject()(localesService: LocalesAdminService,
                                              foodBrowsingAdminService: FoodBrowsingAdminService,
                                              problemCheckerService: ProblemCheckerService,
                                              actorSystem: ActorSystem,
                                              configuration: Configuration,
                                              context: ExecutionContext) {

  private val throttleRateMs = configuration.get[Int](s"intake24.asyncProblemsPrecacher.throttleRateMs")
  private val maxConcurrentTasks = configuration.get[Int](s"intake24.asyncProblemsPrecacher.maxConcurrentTasks")
  private val maxRecursiveResults = configuration.get[Int](s"intake24.asyncProblemsPrecacher.maxRecursiveResults")

  private sealed trait Task

  private case class VisitLocale(localeId: String) extends Task

  private case class VisitCategory(localeId: String, categoryCode: String) extends Task

  private case class QueryFood(localeId: String, foodCode: String) extends Task

  private case class QueryCategory(localeId: String, foodCode: String) extends Task

  private val queue = new ConcurrentLinkedQueue[Task]()

  private val logger = LoggerFactory.getLogger(classOf[AsynchronousProblemsPrecacher])

  private val finished = new AtomicBoolean(false)

  /*private val scheduler = new ThrottlingScheduler {
    def run(f: => Unit): Unit = actorSystem.scheduler.scheduleOnce(throttleRateMs.milliseconds)(f)
  }*/

  private def logErrors[T](result: Either[AnyError, T])(block: T => Unit): Unit = {
    result match {
      case Right(v) => block(v)
      case Left(error) => logger.error(s"Database error", error.exception)
    }
  }

  private def processNextTask(): Unit = {

    val nextTask = queue.poll()

    if (nextTask == null)
      finished.set(true)
    else {
      nextTask match {
        case VisitLocale(localeId) =>
          logErrors(foodBrowsingAdminService.getRootCategories(localeId)) {
            categories =>
              categories.foreach {
                categoryHeader =>
                  queue.add(VisitCategory(localeId, categoryHeader.code))
                  logger.debug(s"Queued root category visit: ${categoryHeader.code} (${categoryHeader.englishDescription})")
              }
          }

        case VisitCategory(localeId, categoryCode) =>
          logErrors(foodBrowsingAdminService.getCategoryContents(localeId, categoryCode)) {
            contents =>
              contents.foods.foreach {
                foodHeader =>
                  queue.add(QueryFood(localeId, foodHeader.code))
                  logger.debug(s"Queued food query: ${foodHeader.code} (${foodHeader.englishDescription})")
              }

              contents.subcategories.foreach {
                categoryHeader =>
                  queue.add(VisitCategory(localeId, categoryHeader.code))
                  logger.debug(s"Queued subcategory visit: ${categoryHeader.code} (${categoryHeader.englishDescription})")
              }
          }

          queue.add(QueryCategory(localeId, categoryCode))
          logger.debug(s"Queued category query: $categoryCode")


        case QueryFood(localeId, foodCode) => logErrors(problemCheckerService.getFoodProblems(foodCode, localeId)) {
          _ =>
            logger.debug(s"Cached food $localeId/$foodCode")
        }

        case QueryCategory(localeId, categoryCode) => logErrors(problemCheckerService.getRecursiveCategoryProblems(categoryCode, localeId, maxRecursiveResults)) {
          _ =>
            logger.debug(s"Cached category $localeId/$categoryCode")
        }
      }

      actorSystem.scheduler.scheduleOnce(throttleRateMs.milliseconds) {
        processNextTask()
      }
    }
  }


  localesService.listLocales() match {
    case Right(locales) =>
      locales.foreach {
        l =>
          val id = l._1
          queue.add(VisitLocale(l._1))
          logger.debug(s"Queued locale $id")
      }
  }

  Range(0, maxConcurrentTasks).foreach { _ => actorSystem.scheduler.scheduleOnce(0.milliseconds) { processNextTask() } }

  def precacheFinished(): Boolean = finished.get()

}