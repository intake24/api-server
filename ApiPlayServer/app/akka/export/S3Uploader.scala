package akka.export

import akka.actor.Actor
import play.api.Logger




/*
class S3Uploader extends Actor {
  def receive: Receive = {
    exportService.getSurveySubmissions(currentTask.surveyId, Some(currentTask.dateFrom), Some(currentTask.dateTo), currentOffset, batchSize, None) match {
      case Right(submissions) => {

        Logger.info(s"Received next batch of ${submissions.size} submissions")

        if (submissions.size > 0) {
          currentOffset += submissions.size
          Logger.info(s"Writing ${submissions.size} submissions to file...")

          fileWriter ! WriteNextBatch(submissions)
        } else {
          Logger.info("All submissions for the curent task processed")

          fileWriter ! Finalise
        }
      }

      case Left(e) => {
        Logger.error("Error :(", e.exception)
      }
    }


  }
}
*/