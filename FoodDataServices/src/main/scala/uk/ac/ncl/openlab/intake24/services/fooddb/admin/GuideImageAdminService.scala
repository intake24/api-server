package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.{DependentUpdateError, LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.GuideImage

trait GuideImageAdminService extends GuideImageService {
  
  def listGuideImages(): Either[UnexpectedDatabaseError, Seq[GuideHeader]]
  
  def deleteAllGuideImages(): Either[UnexpectedDatabaseError, Unit]
  
  def createGuideImages(guideImages: Seq[GuideImage]): Either[UnexpectedDatabaseError, Unit]

  def updateGuideSelectionImage(id: String, selectionImageId: Long): Either[DependentUpdateError, Unit]
}