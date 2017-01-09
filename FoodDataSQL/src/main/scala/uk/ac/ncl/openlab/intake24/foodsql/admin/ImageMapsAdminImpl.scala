package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import anorm.{BatchSql, Macro, NamedParameter, SQL, SqlParser}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.errors._
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader

@Singleton
class ImageMapsAdminStandaloneImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends ImageMapsAdminImpl

trait ImageMapsAdminImpl extends ImageMapsAdminService with FoodDataSqlService with SqlResourceLoader {

  private val logger = LoggerFactory.getLogger(classOf[ImageMapsAdminImpl])

  private def buildObjectParams(imageMap: NewImageMapRecord): Seq[Seq[NamedParameter]] = {
    imageMap.objects.toSeq.map {
      case (objectId, obj) =>
        val navIndex = imageMap.navigation.indexOf(objectId)
        val outline = s"{${obj.outline.mkString(",")}}"
        Seq[NamedParameter]('id -> objectId.toLong, 'image_map_id -> imageMap.id, 'navigation_index -> navIndex, 'description -> obj.description, 'outline_coordinates -> outline, 'overlay_image_id -> obj.overlayImageId)
    }
  }

  override def listImageMaps(): Either[UnexpectedDatabaseError, Seq[ImageMapHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT id, description FROM image_maps").executeQuery().as(Macro.namedParser[ImageMapHeader].*))
  }

  override def getImageMapBaseImageSourceId(id: String): Either[LookupError, Long] = tryWithConnection {
    implicit conn =>
      val result = SQL("SELECT source_id FROM image_maps AS im JOIN processed_images AS pi ON im.base_image_id=pi.id where im.id={id}")
        .on('id -> id)
        .executeQuery()
        .as(SqlParser.long("source_id").singleOpt)

      result match {
        case Some(id) => Right(id)
        case None => Left(RecordNotFound(new RuntimeException(s"image map $id")))
      }
  }

  override def createImageMaps(imageMaps: Seq[NewImageMapRecord]): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        logger.debug(s"Creating image maps ${imageMaps.map(_.id).mkString(", ")}")

        val imageMapParams = imageMaps.map {
          imageMap =>
            Seq[NamedParameter]('id -> imageMap.id, 'description -> imageMap.description, 'base_image_id -> imageMap.baseImageId)
        }

        tryWithConstraintCheck("image_maps_pkey", e => DuplicateCode(e)) {
          BatchSql("INSERT INTO image_maps VALUES({id},{description},{base_image_id})", imageMapParams.head, imageMapParams.tail: _*).execute()

          val imageMapObjectParams = imageMaps.flatMap {
            imageMap =>
              buildObjectParams(imageMap)
          }

          BatchSql("INSERT INTO image_map_objects VALUES({id},{image_map_id},{description},{navigation_index},{outline_coordinates}::double precision[],{overlay_image_id})", imageMapObjectParams.head, imageMapObjectParams.tail: _*).execute()

          Right(())
        }
      }
  }

  lazy val imageMapObjectsUpdateQuery = sqlFromResource("admin/update_image_map_objects.sql")

  override def updateImageMap(imageMapId: String, update: NewImageMapRecord): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        logger.debug(s"Updating image map $imageMapId")

        tryWithConstraintsCheck(Map("image_maps_pkey" -> (e => DuplicateCode(e)), "guide_image_object_fk" -> (e => StillReferenced(e)))) {
          logger.debug(s"Updating image map record")

          SQL("UPDATE image_maps SET id={new_id},description={description},base_image_id={base_image_id} WHERE id={id}")
            .on('new_id -> update.id, 'description -> update.description, 'base_image_id -> update.baseImageId)
            .executeUpdate()

          logger.debug("Updating image map objects")

          // This is a bit tricky because there could be guide images referencing the image map objects
          // The best we can do to ensure integrity is attempt to update existing objects where they exist,
          // create new ones where they don't and finally attempt to delete existing objects that are no longer
          // in this image map triggering foreign key deletion restriction

          val imageMapObjectParams = buildObjectParams(update)

          BatchSql(imageMapObjectsUpdateQuery, imageMapObjectParams.head, imageMapObjectParams.tail: _*).execute()

          SQL("DELETE FROM image_map_objects WHERE image_map_id={image_map_id} AND id NOT IN({new_object_ids})").on('image_map_id -> imageMapId, 'new_object_ids -> update.objects.keySet.toSeq).execute()

          Right(())
        }
      }
  }
}
