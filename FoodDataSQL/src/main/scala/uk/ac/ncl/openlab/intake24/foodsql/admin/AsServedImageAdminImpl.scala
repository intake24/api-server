package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.AsServedHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.user.AsServedImageUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.AsServedSetV1
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSet
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSetWithPaths
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageWithPaths

@Singleton
class AsServedImageAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends AsServedImageAdminImpl

trait AsServedImageAdminImpl extends AsServedImageAdminService with FoodDataSqlService with SqlResourceLoader {

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
        withTransaction {
          logger.debug("Writing " + sets.size + " as served sets to database")

          val asServedSetParams = sets.map(set => Seq[NamedParameter]('id -> set.id, 'description -> set.description))

          tryWithConstraintCheck("as_served_sets_pk", DuplicateCode) {
            batchSql("INSERT INTO as_served_sets VALUES({id}, {description})", asServedSetParams).execute()

            val asServedImageParams = sets.flatMap(set => set.images.map(image => Seq[NamedParameter]('as_served_set_id -> set.id, 'weight -> image.weight,
              'main_image_id -> image.mainImageId, 'thumbnail_id -> image.thumbnailId)))

            if (!asServedImageParams.isEmpty) {
              logger.debug("Writing " + asServedImageParams.size + " as served images to database")
              batchSql("INSERT INTO as_served_images VALUES(DEFAULT,{as_served_set_id},{weight},'',{main_image_id},{thumbnail_id})", asServedImageParams).execute()
            } else
              logger.debug("As served sets in createAsServedSets request contain no image references")

            Right(())
          }
        }
      } else {
        logger.debug("createAsServedSets request with empty as served set list")
        Right(())
      }
  }
  /*
   *   id serial NOT NULL,
  as_served_set_id character varying(32) NOT NULL,
  weight double precision NOT NULL,
  url character varying(512) NOT NULL,
  image_id integer,
  thumbnail_image_id integer,
  
  weight, image_id, p1.path as image_path, thumbnail_image_id, p2.path as thumbnail_image_path FROM as_served_images
   */
  private case class AsServedImageRow(weight: Double, image_id: Long, image_path: String, thumbnail_image_id: Long, thumbnail_image_path: String)

  val query = sqlFromResource("admin/get_as_served_set.sql")

  def getAsServedSet(id: String): Either[LookupError, AsServedSetWithPaths] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT description FROM as_served_sets WHERE id={id}").on('id -> id).executeQuery().as(SqlParser.str("description").singleOpt) match {
          case Some(description) => {
            val images = SQL(query).on('as_served_set_id -> id).as(Macro.namedParser[AsServedImageRow].*).map {
              row =>
                AsServedImageWithPaths(row.image_id, row.image_path, row.thumbnail_image_id, row.thumbnail_image_path, row.weight)
            }
            Right(AsServedSetWithPaths(id, description, images))
          }
          case None => Left(RecordNotFound(new RuntimeException(s"As served set $id not found")))
        }
      }
  }

  def updateAsServedSet(id: String, update: AsServedSet): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        if (SQL("UPDATE as_served_sets SET id={new_id},description={description} WHERE id={id}").on('id -> id, 'new_id -> update.id, 'description -> update.description).executeUpdate() != 1)
          Left(RecordNotFound(new RuntimeException(s"As served set $id not found")))
        else {
          SQL("DELETE FROM as_served_images WHERE as_served_set_id={id}").on('id -> update.id).execute()

          if (update.images.nonEmpty) {
            val imageParams = update.images.map {
              image => Seq[NamedParameter]()
            }

            batchSql("INSERT INTO as_served_images VALUES(DEFAULT,{as_served_set_it},{weight},'',{image_id},{thumbnail_image_id})", imageParams).execute()
          }

          Right(())
        }
      }
  }
}
