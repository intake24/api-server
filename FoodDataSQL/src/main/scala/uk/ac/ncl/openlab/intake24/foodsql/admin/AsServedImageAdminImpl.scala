package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.AsServedHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.user.AsServedImageUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import uk.ac.ncl.openlab.intake24.AsServedSet
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.name.Named

class AsServedImageAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends AsServedImageAdminImpl

trait AsServedImageAdminImpl extends AsServedImageAdminService with AsServedImageUserImpl {

  private val logger = LoggerFactory.getLogger(classOf[AsServedImageAdminImpl])

  def listAsServedSets(): Either[DatabaseError, Map[String, AsServedHeader]] = tryWithConnection {
    implicit conn =>
      val headers = SQL("""SELECT id, description FROM as_served_sets""").executeQuery().as(Macro.namedParser[AsServedHeader].*)

      Right(headers.map(h => (h.id, h)).toMap)
  }

  def deleteAllAsServedSets(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM as_served_sets").execute()

      Right(())
  }

  def createAsServedSets(sets: Seq[AsServedSet]): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>

      if (sets.nonEmpty) {
        conn.setAutoCommit(false)
        logger.debug("Writing " + sets.size + " as served sets to database")

        val asServedSetParams = sets.map(set => Seq[NamedParameter]('id -> set.id, 'description -> set.description))

        tryWithConstraintCheck("as_served_sets_pk", DuplicateCode) {
          batchSql("INSERT INTO as_served_sets VALUES({id}, {description})", asServedSetParams).execute()

          val asServedImageParams = sets.flatMap(set => set.images.map(image => Seq[NamedParameter]('as_served_set_id -> set.id, 'weight -> image.weight, 'url -> image.url)))

          if (!asServedImageParams.isEmpty) {
            logger.debug("Writing " + asServedImageParams.size + " as served images to database")
            batchSql("INSERT INTO as_served_images VALUES(DEFAULT, {as_served_set_id}, {weight}, {url})", asServedImageParams).execute()
          } else
            logger.debug("As served sets in createAsServedSets request contain no image references")

          conn.commit() 
          Right(())
        }

      } else {
        logger.debug("createAsServedSets request with empty as served set list")
        Right(())
      }
  }
}
