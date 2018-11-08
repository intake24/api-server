package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.api.data.GuideHeader
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService

case class NewGuideImageRecord(id: String, description: String, imageMapId: String, selectionImageId: Long, objectWeights: Map[Long, Double])

case class GuideImageMeta(id: String, description: String)


// case class GuideImageMapObject(weight: Double, description: String, navigationIndex: Int, outlineCoordinates: Seq[Double])

case class GuideImageMapObject(objectId: Int, weight: Double)

trait GuideImageAdminService extends GuideImageService {

  def listGuideImages(): Either[UnexpectedDatabaseError, Seq[GuideHeader]]

  def deleteAllGuideImages(): Either[UnexpectedDatabaseError, Unit]

  def createGuideImages(guideImages: Seq[NewGuideImageRecord]): Either[DependentCreateError, Unit]

  def updateGuideSelectionImage(id: String, selectionImageId: Long): Either[DependentUpdateError, Unit]

  def updateGuideImageMeta(id: String, meta: GuideImageMeta): Either[UpdateError, GuideImageMeta]

  def updateGuideImageObjects(id: String, objects: Seq[GuideImageMapObject]): Either[UpdateError, Unit]

  def getImageMapId(id: String): Either[LookupError, String]

}
