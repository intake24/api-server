package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.AsServedHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.AsServedImageServiceImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait AsServedImageAdminServiceImpl extends AsServedImageServiceImpl {
  
  def allAsServedSets(): Either[DatabaseError, Seq[AsServedHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT id, description FROM as_served_sets ORDER BY description ASC""").executeQuery().as(Macro.namedParser[AsServedHeader].*))
  }
}
