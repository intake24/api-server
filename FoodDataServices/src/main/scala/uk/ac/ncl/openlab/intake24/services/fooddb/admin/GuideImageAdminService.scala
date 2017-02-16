package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.errors.{DependentCreateError, DependentUpdateError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService

case class NewGuideImageRecord(id: String, description: String, imageMapId: String, selectionImageId: Long, objectWeights: Map[Long, Double])

trait GuideImageAdminService extends GuideImageService {

  def listGuideImages(): Either[UnexpectedDatabaseError, Seq[GuideHeader]]

  def deleteAllGuideImages(): Either[UnexpectedDatabaseError, Unit]

  def createGuideImages(guideImages: Seq[NewGuideImageRecord]): Either[DependentCreateError, Unit]

  def updateGuideSelectionImage(id: String, selectionImageId: Long): Either[DependentUpdateError, Unit]
}
