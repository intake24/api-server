/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package controllers.pub

import java.net.URL
import java.util.Date

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectListing, S3ObjectSummary}
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.mvc._

import scala.collection.JavaConverters
import scala.concurrent.{ExecutionContext, Future}

class DatabaseSnapshotDownloadController @Inject()(s3: AmazonS3,
                                                   configuration: Configuration,
                                                   val controllerComponents: ControllerComponents,
                                                   implicit val executionContext: ExecutionContext) extends BaseController {

  val bucketName = configuration.get[String]("intake24.dbSnapshots.s3.bucket")
  val prefix = configuration.get[String]("intake24.dbSnapshots.s3.foodsPrefix")

  val logger = LoggerFactory.getLogger(classOf[DatabaseSnapshotDownloadController])

  case class NewestObject(date: Date, key: String)

  private def findNewestInBatch(objects: TraversableOnce[S3ObjectSummary], newestSoFar: Option[NewestObject]): Option[NewestObject] =
    objects.foldLeft(newestSoFar) {
      case (result, summary) =>
        result match {
          case None => Some(NewestObject(summary.getLastModified, summary.getKey))
          case Some(newest) =>
            val thisDate = summary.getLastModified
            if (thisDate.after(newest.date))
              Some(NewestObject(thisDate, summary.getKey))
            else
              Some(newest)
        }
    }

  private def getNewestObjectUrl(previousListing: Option[ObjectListing] = None, newestSoFar: Option[NewestObject] = None): Option[URL] = {
    val listing = previousListing match {
      case Some(prev) => s3.listNextBatchOfObjects(prev)
      case None => s3.listObjects(bucketName, prefix)
    }

    logger.debug(s"Listing for $bucketName$prefix contains ${listing.getObjectSummaries.size()} objects")

    val newestObject = findNewestInBatch(JavaConverters.asScalaBuffer(listing.getObjectSummaries), newestSoFar)

    if (listing.isTruncated)
      getNewestObjectUrl(Some(listing), newestObject)
    else newestObject.map(obj => s3.getUrl(bucketName, obj.key))
  }

  def getFoodsSnapshotUrl() = Action.async {
    Future {
      getNewestObjectUrl() match {
        case Some(url) => SeeOther(url.toString)
        case None => NotFound
      }
    }
  }
}
