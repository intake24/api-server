package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import anorm._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

@Singleton
class AsServedSetsAdminStandaloneImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends AsServedSetsAdminImpl

trait AsServedSetsAdminImpl extends AsServedSetsAdminService with SqlDataService with SqlResourceLoader {

  private val logger = LoggerFactory.getLogger(classOf[AsServedSetsAdminImpl])

  def listAsServedSets(): Either[UnexpectedDatabaseError, Map[String, AsServedHeader]] = tryWithConnection {
    implicit conn =>
      val headers = SQL("""SELECT id, description FROM as_served_sets""").executeQuery().as(Macro.namedParser[AsServedHeader].*)

      Right(headers.map(h => (h.id, h)).toMap)
  }

  def deleteAllAsServedSets(): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM as_served_sets").execute()

      Right(())
  }

  def createAsServedSets(sets: Seq[NewAsServedSetRecord]): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      if (sets.nonEmpty) {
        withTransaction {
          logger.debug("Writing " + sets.size + " as served sets to database")

          val asServedSetParams = sets.map(set => Seq[NamedParameter]('id -> set.id, 'selection_image_id -> set.selectionImageId, 'description -> set.description))

          tryWithConstraintCheck("as_served_sets_pk", DuplicateCode) {
            batchSql("INSERT INTO as_served_sets VALUES({id},{description},{selection_image_id})", asServedSetParams).execute()

            val asServedImageParams = sets.flatMap(set => set.images.map(image => Seq[NamedParameter]('as_served_set_id -> set.id, 'weight -> image.weight,
              'main_image_id -> image.mainImageId, 'thumbnail_id -> image.thumbnailId)))

            if (asServedImageParams.nonEmpty) {
              logger.debug("Writing " + asServedImageParams.size + " as served images to database")
              batchSql("INSERT INTO as_served_images VALUES(DEFAULT,{as_served_set_id},{weight},{main_image_id},{thumbnail_id})", asServedImageParams).execute()
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

  private case class AsServedImageRow(image_path: String, thumbnail_image_path: String, weight: Double, source_id: Long)

  private case class AsServedSetRow(description: String)

  private lazy val setQuery = sqlFromResource("admin/get_as_served_set.sql")

  private lazy val imagesQuery = sqlFromResource("admin/get_as_served_images.sql")

  def getAsServedSetWithPaths(id: String): Either[LookupError, AsServedSetWithPaths] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL(setQuery).on('id -> id).executeQuery().as(Macro.namedParser[AsServedSetRow].singleOpt) match {
          case Some(row) =>
            val images = SQL(imagesQuery).on('as_served_set_id -> id).as(Macro.namedParser[AsServedImageRow].*).map {
              row =>
                AsServedImageWithPaths(row.source_id, row.image_path, row.thumbnail_image_path, row.weight)
            }
            Right(AsServedSetWithPaths(id, row.description, images))
          case None => Left(RecordNotFound(new RuntimeException(s"As served set $id not found")))
        }
      }
  }

  private case class AsServedSetRecordRow(description: String, selection_image_id: Long)

  private case class ImageRecordRow(id: Long, image_id: Long, thumbnail_image_id: Long, weight: Double)

  private lazy val setRecordQuery = sqlFromResource("admin/get_as_served_set_record.sql")

  private lazy val imageRecordsQuery = sqlFromResource("admin/get_as_served_image_records.sql")

  def getAsServedSetRecord(id: String): Either[LookupError, AsServedSetRecord] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL(setRecordQuery).on('id -> id).executeQuery().as(Macro.namedParser[AsServedSetRecordRow].singleOpt) match {
          case Some(row) =>
            val images = SQL(imageRecordsQuery).on('as_served_set_id -> id).as(Macro.namedParser[ImageRecordRow].*).map {
              row =>
                AsServedImageRecord(row.id, row.image_id, row.thumbnail_image_id, row.weight)
            }
            Right(AsServedSetRecord(id, row.description, row.selection_image_id, images))

          case None => Left(RecordNotFound(new RuntimeException(s"As served set $id not found")))
        }
      }
  }

  private case class PortableAsServedImageRow(weight: Double, source_path: String, source_thumbnail_path: String, keywords: Array[String], image_path: String, thumbnail_image_path: String)

  private case class PortableAsServedSetRow(description: String, selection_source_path: String, selection_image_path: String)

  val portableSetQuery = sqlFromResource("admin/get_portable_as_served_set.sql")

  val portableImagesQuery = sqlFromResource("admin/get_portable_as_served_images.sql")

  def getPortableAsServedSet(id: String): Either[LookupError, PortableAsServedSet] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL(portableSetQuery).on('id -> id).executeQuery().as(Macro.namedParser[PortableAsServedSetRow].singleOpt) match {
          case Some(row) =>
            val images = SQL(portableImagesQuery).on('as_served_set_id -> id).as(Macro.namedParser[PortableAsServedImageRow].*).map {
              row =>
                PortableAsServedImage(row.source_path, row.source_thumbnail_path, row.keywords, row.image_path, row.thumbnail_image_path, row.weight)
            }
            Right(PortableAsServedSet(id, row.description, row.selection_source_path, row.selection_image_path, images))
          case None => Left(RecordNotFound(new RuntimeException(s"As served set $id not found")))
        }
      }
  }

  def updateAsServedSet(id: String, update: NewAsServedSetRecord): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        if (SQL("UPDATE as_served_sets SET id={new_id},selection_image_id={selection_image_id},description={description} WHERE id={id}")
          .on('id -> id, 'new_id -> update.id, 'selection_image_id -> update.selectionImageId, 'description -> update.description).executeUpdate() != 1)
          Left(RecordNotFound(new RuntimeException(s"As served set $id not found")))
        else {
          SQL("DELETE FROM as_served_images WHERE as_served_set_id={id}").on('id -> update.id).execute()

          if (update.images.nonEmpty) {
            val imageParams = update.images.map {
              image => Seq[NamedParameter]('as_served_set_id -> update.id, 'weight -> image.weight, 'image_id -> image.mainImageId, 'thumbnail_image_id -> image.thumbnailId)
            }

            batchSql("INSERT INTO as_served_images VALUES(DEFAULT,{as_served_set_id},{weight},{image_id},{thumbnail_image_id})", imageParams).execute()
          }

          Right(())
        }
      }
  }
}
