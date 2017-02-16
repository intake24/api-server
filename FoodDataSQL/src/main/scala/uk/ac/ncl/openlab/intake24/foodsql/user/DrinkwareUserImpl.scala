package uk.ac.ncl.openlab.intake24.foodsql.user

import java.sql.Connection

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, SqlParser, sqlToSimple}
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.{DrinkScale, DrinkwareSet, VolumeFunction}

trait DrinkwareUserImpl extends DrinkwareService with SqlDataService with SqlResourceLoader {

  protected case class DrinkwareResultRow(id: String, scale_id: Long, description: String, guide_image_id: String,
                                          width: Int, height: Int, empty_level: Int, full_level: Int, choice_id: Int, base_image_url: String,
                                          overlay_image_url: String)

  protected case class VolumeSampleResultRow(scale_id: Long, fill: Double, volume: Double)

  private lazy val drinkwareScalesQuery = sqlFromResource("user/drinkware_scales.sql")

  private lazy val drinkwareVolumeSamplesQuery = sqlFromResource("user/drinkware_volume_samples.sql")

  def getDrinkwareSet(id: String): Either[LookupError, DrinkwareSet] = tryWithConnection {
    implicit conn =>

      conn.setAutoCommit(false)
      conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)

      val validation = SQL("SELECT true AS v FROM drinkware_sets WHERE id={drinkware_id}").on('drinkware_id -> id).executeQuery().as(SqlParser.bool("v").singleOpt)

      if (validation.isEmpty) {
        conn.commit()
        Left(RecordNotFound(new RuntimeException(id)))
      } else {
        val result = SQL(drinkwareScalesQuery).on('drinkware_id -> id).executeQuery().as(Macro.namedParser[DrinkwareResultRow].*)

        val scale_ids = result.map(_.scale_id)

        val volume_sample_results = SQL(drinkwareVolumeSamplesQuery).on('scale_ids -> scale_ids).executeQuery().as(Macro.namedParser[VolumeSampleResultRow].*)

        val scales = result.map(r => DrinkScale(r.choice_id, r.base_image_url, r.overlay_image_url, r.width, r.height, r.empty_level, r.full_level,
          VolumeFunction(volume_sample_results.filter(_.scale_id == r.scale_id).map(s => (s.fill, s.volume)))))

        conn.commit()
        Right(DrinkwareSet(id, result.head.description, result.head.guide_image_id, scales))
      }
  }
}