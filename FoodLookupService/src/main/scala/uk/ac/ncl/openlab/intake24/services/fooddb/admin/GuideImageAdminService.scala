package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait GuideImageAdminService extends GuideImageService {
  
  def allGuideImages(): Either[DatabaseError, Seq[GuideHeader]]
}