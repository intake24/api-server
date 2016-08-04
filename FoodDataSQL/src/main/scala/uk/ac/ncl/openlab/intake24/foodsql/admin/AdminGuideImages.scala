package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.GuideHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.UserGuideImages

trait AdminGuideImages extends UserGuideImages {
  
  def allGuideImages(): Seq[GuideHeader] = tryWithConnection {
    implicit conn =>
      SQL("""SELECT id, description from guide_images ORDER BY description ASC""").executeQuery().as(Macro.namedParser[GuideHeader].*)
  }
}
