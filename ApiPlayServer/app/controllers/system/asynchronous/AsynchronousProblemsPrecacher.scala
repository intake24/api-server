package controllers.system.asynchronous

import java.util.concurrent.{ConcurrentLinkedDeque, ConcurrentLinkedQueue, CountDownLatch}
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import modules.ProblemCheckerService
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{FoodBrowsingAdminService, LocalesAdminService}

import scala.collection.immutable.Stack
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random


@Singleton
class AsynchronousProblemsPrecacher @Inject()(localesService: LocalesAdminService,
                                              foodBrowsingAdminService: FoodBrowsingAdminService,
                                              problemCheckerService: ProblemCheckerService,
                                              actorSystem: ActorSystem,
                                              configuration: Configuration,
                                              implicit val executionContext: ExecutionContext) {

  private val throttleRateMs = configuration.get[Int](s"intake24.asyncProblemsPrecacher.throttleRateMs")
  private val maxConcurrentTasks = configuration.get[Int](s"intake24.asyncProblemsPrecacher.maxConcurrentTasks")
  private val maxRecursiveResults = configuration.get[Int](s"intake24.asyncProblemsPrecacher.maxRecursiveResults")

  private sealed trait Task

  private case class VisitLocale(localeId: String) extends Task

  private case class VisitCategory(localeId: String, categoryCode: String) extends Task

  private case class QueryFood(localeId: String, foodCode: String) extends Task

  private case class QueryCategory(localeId: String, foodCode: String) extends Task

  private val logger = LoggerFactory.getLogger(classOf[AsynchronousProblemsPrecacher])

  private var countDownLatch: CountDownLatch = null

  private def logError(error: AnyError): Unit = logger.error(s"Database error: ${error.toString}", error.exception)

  private def processNextTask(workerInfo: String, startTime: Long, queue: List[Task], cachedCategories: Set[String]): Unit =
    queue match {
      case Nil =>
        countDownLatch.countDown()
        val time = (System.currentTimeMillis() - startTime).milliseconds
        logger.info(s"[$workerInfo] finished in ${time.toMinutes} minutes")

      case task :: tasks =>

        // logger.debug(queue.mkString(", "))

        val cc = scala.collection.mutable.Set[String]()
        cc ++= cachedCategories


        val newQueue = task match {
          case VisitLocale(localeId) =>

            //logger.debug(s"[$workerInfo] Visit locale: $localeId")

            foodBrowsingAdminService.getRootCategories(localeId) match {
              case Right(categories) =>

                categories.foldLeft(tasks) {
                  (acc, h) => VisitCategory(localeId, h.code) :: acc
                }

              case Left(e) =>
                logError(e)
                tasks
            }

          case VisitCategory(localeId, categoryCode) =>

            // logger.debug(s"[$workerInfo] Visit category: $categoryCode")

            foodBrowsingAdminService.getCategoryContents(categoryCode, localeId) match {
              case Right(contents) =>

                val q = contents.subcategories.foldLeft(QueryCategory(localeId, categoryCode) :: tasks) {
                  (acc, h) => VisitCategory(localeId, h.code) :: acc
                }

                contents.foods.foldLeft(q) {
                  (acc, h) => QueryFood(localeId, h.code) :: acc
                }

              case Left(e) =>
                logError(e)
                tasks
            }

          case QueryFood(localeId, foodCode) =>
            // logger.debug(s"[$workerInfo] Query food: $foodCode")
            problemCheckerService.getFoodProblems(foodCode, localeId) match {
              case Right(_) => tasks
              case Left(e) =>
                logError(e)
                tasks
            }

          case QueryCategory(localeId, categoryCode) =>
            // logger.debug(s"[$workerInfo] Query category: $categoryCode")
            problemCheckerService.getRecursiveCategoryProblems(categoryCode, localeId, maxRecursiveResults) match {
              case Right(_) =>
                cc += categoryCode
                tasks
              case Left(e) =>
                logError(e)
                tasks
            }
        }

        //actorSystem.scheduler.scheduleOnce(throttleRateMs.milliseconds) {
        processNextTask(workerInfo, startTime, newQueue, cc.toSet)
      //}
    }

  localesService.listLocales() match {
    case Right(locales) =>
      logger.debug(locales.keySet.toSeq.sorted.mkString(", "))
      locales.keySet.toSeq.sorted.foreach {
        case id => actorSystem.scheduler.scheduleOnce(Random.nextInt(throttleRateMs * 4).milliseconds) {
          logger.info(s"[$id] Started")
          processNextTask(s"$id", System.currentTimeMillis(), List(VisitLocale(id)), Set())
        }
      }
      countDownLatch = new CountDownLatch(locales.size)
    case Left(e) =>
      logError(e)
  }

  def precacheFinished(): Boolean = countDownLatch != null && countDownLatch.getCount == 0

}