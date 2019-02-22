package controllers.system.asynchronous

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import modules.ProblemCheckerService
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{FoodBrowsingAdminService, LocalesAdminService}

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
  private val maxRecursiveResults = configuration.get[Int](s"intake24.asyncProblemsPrecacher.maxRecursiveResults")
  private val enabledLocales = configuration.get[Seq[String]](s"intake24.asyncProblemsPrecacher.enabledLocales")

  private sealed trait Task

  private case class VisitLocale(localeId: String) extends Task

  private case class VisitCategory(localeId: String, categoryCode: String) extends Task

  private case class QueryFood(localeId: String, foodCode: String) extends Task

  private case class QueryCategory(localeId: String, foodCode: String) extends Task

  private val logger = LoggerFactory.getLogger(classOf[AsynchronousProblemsPrecacher])

  private val countDownLatch = new AtomicReference[CountDownLatch](null)

  private def logError(error: DatabaseError): Unit = logger.error(s"Database error: ${error.toString}", error.exception)

  private def processNextTask(workerInfo: String, startTime: Long, queue: List[Task]): Unit = {

    queue match {
      case Nil =>
        countDownLatch.get().countDown()

        val time = (System.currentTimeMillis() - startTime).milliseconds
        logger.info(s"[$workerInfo] finished in ${time.toMinutes} minutes")

        if (countDownLatch.get().getCount == 0)
          problemCheckerService.disablePrecacheWarnings()

      case task :: tasks =>

        val newTasks = task match {
          case VisitLocale(localeId) =>
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
            foodBrowsingAdminService.getCategoryContents(categoryCode, localeId) match {
              case Right(contents) =>

                val newTasks1 = contents.subcategories.foldLeft(QueryCategory(localeId, categoryCode) :: tasks) {
                  (acc, h) => VisitCategory(localeId, h.code) :: acc
                }

                contents.foods.foldLeft(newTasks1) {
                  (acc, h) => QueryFood(localeId, h.code) :: acc
                }

              case Left(e) =>
                logError(e)
                tasks
            }

          case QueryFood(localeId, foodCode) =>
            problemCheckerService.getFoodProblems(foodCode, localeId) match {
              case Right(_) =>
                tasks
              case Left(e) =>
                logError(e)
                tasks
            }

          case QueryCategory(localeId, categoryCode) =>
            problemCheckerService.getRecursiveCategoryProblems(categoryCode, localeId, maxRecursiveResults) match {
              case Right(_) =>
                tasks
              case Left(e) =>
                logError(e)
                tasks
            }
        }

        actorSystem.scheduler.scheduleOnce(throttleRateMs.milliseconds) {
          processNextTask(workerInfo, startTime, newTasks)
        }

    }
  }

  localesService.listLocales() match {
    case Right(knownLocales) =>

      val locales = enabledLocales match {
        case Seq("*") => knownLocales.keySet.toSeq.sorted
        case _ => knownLocales.keySet.toSeq.sorted.filter(enabledLocales.contains)
      }

      if (locales.nonEmpty) {
        problemCheckerService.enablePrecacheWarnings()

        locales.foreach {
          case id => actorSystem.scheduler.scheduleOnce(Random.nextInt(throttleRateMs * 4).milliseconds) {
            logger.info(s"[$id] Started")
            processNextTask(s"$id", System.currentTimeMillis(), List(VisitLocale(id)))
          }
        }
        countDownLatch.set(new CountDownLatch(locales.size))

      } else {
        logger.warn("Problems precacher locales list empty -- is this intentional?")
      }

    case Left(e) =>
      logError(e)
  }

  def precacheFinished(): Boolean = {
    val latch = countDownLatch.get()
    latch != null && latch.getCount() == 0
  }

}