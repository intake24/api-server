package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.GuideImage

trait GuideImageAdminService extends GuideImageService {
  
  def listGuideImages(): Either[UnexpectedDatabaseError, Map[String, GuideHeader]]
  
  def deleteAllGuideImages(): Either[UnexpectedDatabaseError, Unit]
  
  def createGuideImages(guideImages: Seq[GuideImage]): Either[UnexpectedDatabaseError, Unit]
}