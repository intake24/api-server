package controllers.system.asynchronous

import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorSystem, Props}
import akka.export.ExportTaskActor
import play.api.Logger
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._

import scala.collection.mutable

import scala.concurrent.duration._

case class ExportTask(taskId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, dataScheme: CustomDataScheme,
                      foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], insertBOM: Boolean)

class ExportManager(exportService: DataExportService, batchSize: Int, throttleRateMs: Int, maxActiveTasks: Int) extends Actor {

  val queue = mutable.Queue[ExportTask]()

  var activeTasks = 0

  def maybeStartNextTask() = {
    if (!queue.isEmpty && activeTasks < maxActiveTasks) {
      activeTasks += 1
      val task = queue.dequeue()
      context.actorOf(Props(classOf[ExportTaskActor], exportService, task, batchSize, throttleRateMs))
      Logger.info("Started task, active tasks: " + activeTasks)
    }
  }

  def receive: Receive = {
    case task: ExportTask =>
      queue += task
      Logger.info("Task queued, queue size: " + queue.size)
      maybeStartNextTask()

    case ExportTaskActor.Complete(file) =>
      Logger.info("Yay, task complete: " + file.getAbsolutePath)
      activeTasks -= 1
      maybeStartNextTask()
  }
}


object DataExporterCache {
  def progressKey(taskId: UUID) = s"DataExporter.$taskId.progress"

  def downloadUrlKey(taskId: UUID) = s"DataExporter.$taskId.url"
}

@Singleton
class DataExporterCache @Inject()(cacheApi: CacheApi) {

  def setProgress(taskId: UUID, progress: Int) = cacheApi.set(DataExporterCache.progressKey(taskId), progress, 2 hours)

  def getProgress(taskId: UUID) = cacheApi.get[Int](DataExporterCache.progressKey(taskId))

  def setDownloadUrl(taskId: UUID, url: String) = cacheApi.set(DataExporterCache.downloadUrlKey(taskId), url, 2 hours)

  def getDownloadUrl(taskId: UUID) = cacheApi.get[String](DataExporterCache.downloadUrlKey(taskId))

}

@Singleton
class AsynchronousDataExporter @Inject()(actorSystem: ActorSystem,
                                         exportService: DataExportService,
                                         cache: DataExporterCache,
                                         surveyAdminService: SurveyAdminService,
                                         foodGroupsAdminService: FoodGroupsAdminService) {


  val exportManager = actorSystem.actorOf(Props(classOf[ExportManager], exportService, 50, 50, 2), "ExportManager")

  def queueCsvExport(userId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, insertBOM: Boolean): Either[AnyError, UUID] = {
    for (
      survey <- surveyAdminService.getSurveyParameters(surveyId).right;
      foodGroups <- foodGroupsAdminService.listFoodGroups(survey.localeId).right;
      dataScheme <- surveyAdminService.getCustomDataScheme(survey.schemeId).right;
      localNutrients <- surveyAdminService.getLocalNutrientTypes(survey.localeId).right;
      taskId <- exportService.createExportTask(ExportTaskParameters(userId, surveyId, dateFrom, dateTo)).right)
      yield {

        exportManager ! ExportTask(taskId, surveyId, dateFrom, dateTo, dataScheme, foodGroups, localNutrients, insertBOM)

        taskId
      }
  }
}
