package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.AsServedHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.user.AsServedImageUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import uk.ac.ncl.openlab.intake24.AsServedSet
import org.slf4j.LoggerFactory

trait AsServedImageAdminImpl extends AsServedImageAdminService with AsServedImageUserImpl {

  private val logger = LoggerFactory.getLogger(classOf[AsServedImageAdminImpl])

  def allAsServedSets(): Either[DatabaseError, Seq[AsServedHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT id, description FROM as_served_sets ORDER BY description ASC""").executeQuery().as(Macro.namedParser[AsServedHeader].*))
  }

  def deleteAllAsServedSets(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM as_served_sets").execute()

      Right(())
  }

  def createAsServedSets(sets: Seq[AsServedSet]): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      if (sets.nonEmpty) {
        conn.setAutoCommit(false)
        logger.info("Writing " + sets.size + " as served sets to database")
        
        val asServedSetParams = sets.flatMap(set => Seq[NamedParameter]('id -> set.id, 'description -> set.description))
        BatchSql("""INSERT INTO as_served_sets VALUES({id}, {description})""", asServedSetParams).execute()

        val asServedImageParams = sets.flatMap(set => set.images.flatMap(image => Seq[NamedParameter]('as_served_set_id -> set.id, 'weight -> image.weight, 'url -> image.url)))
        if (!asServedImageParams.isEmpty) {
          logger.info("Writing " + asServedImageParams.size + " as served images to database")
          BatchSql("""INSERT INTO as_served_images VALUES(DEFAULT, {as_served_set_id}, {weight}, {url})""", asServedImageParams).execute()
        } else
          logger.warn("As served sets in createAsServedSets request contain no image references")

        conn.commit()
        Right(())

      } else {
        logger.warn("createAsServedSets request with empty as served set list")
        Right(())
      }
  }
}
