package uk.ac.ncl.openlab.intake24.foodsql.user

import anorm.Macro
import anorm.SQL
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.VolumeFunction
import uk.ac.ncl.openlab.intake24.DrinkScale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService
import anorm.NamedParameter.symbol
import anorm.sqlToSimple
import scala.Right
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService

trait DrinkwareUserImpl extends DrinkwareService with SqlDataService {
  protected case class DrinkwareResultRow(id: String, scale_id: Long, description: String, guide_image_id: String,
    width: Int, height: Int, empty_level: Int, full_level: Int, choice_id: Int, base_image_url: String,
    overlay_image_url: String)

  protected case class VolumeSampleResultRow(scale_id: Long, fill: Double, volume: Double)

  def drinkwareDef(id: String): Either[ResourceError, DrinkwareSet] = tryWithConnection {
    implicit conn =>
      val drinkwareScalesQuery =
        """|SELECT drinkware_sets.id, drinkware_scales.id as scale_id, description, guide_image_id, 
         |       width, height, empty_level, full_level, choice_id, base_image_url, overlay_image_url
         |FROM drinkware_sets JOIN drinkware_scales ON drinkware_set_id = drinkware_sets.id
         |WHERE drinkware_sets.id = {drinkware_id}
         |ORDER by scale_id""".stripMargin

      val result = SQL(drinkwareScalesQuery).on('drinkware_id -> id).executeQuery().as(Macro.namedParser[DrinkwareResultRow].+)

      val scale_ids = result.map(_.scale_id)

      val drinkwareVolumeSamplesQuery =
        """|SELECT drinkware_scale_id as scale_id, fill, volume 
         |FROM drinkware_volume_samples 
         |WHERE drinkware_scale_id IN ({scale_ids}) ORDER BY scale_id, fill""".stripMargin

      val volume_sample_results = SQL(drinkwareVolumeSamplesQuery).on('scale_ids -> scale_ids).executeQuery().as(Macro.namedParser[VolumeSampleResultRow].+)

      val scales = result.map(r => DrinkScale(r.choice_id, r.base_image_url, r.overlay_image_url, r.width, r.height, r.empty_level, r.full_level,
        VolumeFunction(volume_sample_results.filter(_.scale_id == r.scale_id).map(s => (s.fill, s.volume)))))

      Right(DrinkwareSet(id, result.head.description, result.head.guide_image_id, scales))
  }

}