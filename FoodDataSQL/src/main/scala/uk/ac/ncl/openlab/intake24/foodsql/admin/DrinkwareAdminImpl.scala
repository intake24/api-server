package uk.ac.ncl.openlab.intake24.foodsql.admin

import anorm._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{CreateError, DuplicateCode, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.DrinkwareAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import uk.ac.ncl.openlab.intake24.{DrinkwareHeader, DrinkwareSet, DrinkwareSetRecord, VolumeSample}

import javax.sql.DataSource

@Singleton
class DrinkwareAdminImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource, drinkwareService: DrinkwareService) extends DrinkwareAdminService with SqlDataService {

  private val logger = LoggerFactory.getLogger(classOf[DrinkwareAdminImpl])

  def listDrinkwareSets(): Either[UnexpectedDatabaseError, Map[String, DrinkwareHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT id, description FROM drinkware_sets").executeQuery().as(Macro.namedParser[DrinkwareHeader].*).map {
        h => (h.id -> h)
      }.toMap)
  }

  def deleteAllDrinkwareSets(): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug("Deleting existing drinkware definitions")

      SQL("DELETE FROM drinkware_sets").execute()

      Right(())
  }

  def createDrinkwareSets(sets: Seq[DrinkwareSet]): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>

      if (!sets.isEmpty) {
        conn.setAutoCommit(false)

        tryWithConstraintCheck("drinkware_sets_pk", DuplicateCode) {

          logger.debug("Writing " + sets.size + " drinkware sets to database")
          val drinkwareParams = sets.map {
            set =>
              Seq[NamedParameter]('id -> set.id, 'description -> set.description, 'guide_image_id -> set.guideId)
          }

          batchSql("""INSERT INTO drinkware_sets VALUES ({id}, {description}, {guide_image_id})""", drinkwareParams).execute()

          val drinkwareScaleParams = sets.foreach {
            set =>
              set.scales.foreach {
                scale =>
                  val scaleId = SQL("""INSERT INTO drinkware_scales VALUES (DEFAULT, {drinkware_set_id}, {width}, {height}, {empty_level}, {full_level}, {choice_id}, {base_image_url}, {overlay_image_url})""")
                    .on('drinkware_set_id -> set.id, 'width -> scale.width, 'height -> scale.height, 'empty_level -> scale.emptyLevel, 'full_level -> scale.fullLevel,
                      'choice_id -> scale.objectId, 'base_image_url -> scale.baseImagePath, 'overlay_image_url -> scale.overlayImagePath)
                    .executeInsert()
                    .get

                  val volumeSampleParams = scale.volumeSamples.sortBy(_.fl).map {
                    case VolumeSample(fill, volume) =>
                      Seq[NamedParameter]('scale_id -> scaleId, 'fill -> fill, 'volume -> volume)
                  }

                  if (!volumeSampleParams.isEmpty) {
                    logger.debug("Writing " + volumeSampleParams.size + " volume sample records to database")
                    batchSql("""INSERT INTO drinkware_volume_samples VALUES (DEFAULT, {scale_id}, {fill}, {volume})""", volumeSampleParams).execute()
                  } else
                    logger.warn("Drinkware file contains no volume samples")
              }
          }

          conn.commit()
          Right(())
        }
      } else {
        logger.warn("Drinkware file contains no records")
        Right(())
      }
  }

  def createDrinkwareSetRecord(record: DrinkwareSetRecord) = tryWithConnection {
    implicit conn =>
      tryWithConstraintCheck("drinkware_sets_pk", DuplicateCode) {
        SQL("""INSERT INTO drinkware_sets VALUES ({id}, {description}, {guide_image_id})""")
          .on('id -> record.id, 'description -> record.description, 'guide_image_id -> record.guideId)
          .executeInsert()

        Right(())
      }
  }

  def getDrinkwareSet(id: String) = drinkwareService.getDrinkwareSet(id)
}
