package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.GuideHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.GuideImageUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.GuideImageAdminService

trait GuideImageAdminImpl extends GuideImageAdminService with GuideImageUserImpl {
  
  def allGuideImages(): Either[DatabaseError, Seq[GuideHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT id, description from guide_images ORDER BY description ASC""").executeQuery().as(Macro.namedParser[GuideHeader].*))      
  }
}
