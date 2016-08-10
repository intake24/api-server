package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.GuideImage

trait GuideImageAdminService extends GuideImageService {
  
  def allGuideImages(): Either[DatabaseError, Seq[GuideHeader]]
  
  def deleteAllGuideImages(): Either[DatabaseError, Unit]
  
  def createGuideImages(guideImages: Seq[GuideImage]): Either[DatabaseError, Unit]
}