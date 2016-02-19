/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.foodsql

import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser.str
import anorm.sqlToSimple
import net.scran24.fooddef.AsServedHeader
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.GuideHeader
import anorm.Macro
import net.scran24.fooddef.AsServedImage
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.GuideImageWeightRecord
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.DrinkScale
import net.scran24.fooddef.VolumeFunction
import uk.ac.ncl.openlab.intake24.services.ResourceError

trait PortionSizeDataSqlImpl extends SqlDataService {
  case class AsServedResultRow(id: String, description: String, weight: Double, url: String)

  def asServedDef(id: String): Either[ResourceError, AsServedSet] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT as_served_sets.id, description, weight, url
           |FROM as_served_sets JOIN as_served_images ON as_served_sets.id = as_served_set_id
           |WHERE as_served_sets.id = {id} ORDER BY as_served_images.id""".stripMargin

      val result = SQL(query).on('id -> id).executeQuery().as(Macro.namedParser[AsServedResultRow].*)

      if (result.isEmpty)
        Left(ResourceError.ResourceNotFound)
      else {
        val images = result.map(row => AsServedImage(row.url, row.weight))

        Right(AsServedSet(result.head.id, result.head.description, images))
      }
  }

  case class GuideResultRow(image_description: String, object_id: Int, object_description: String, weight: Double)

  def guideDef(id: String): Either[ResourceError, GuideImage] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT guide_images.description as image_description, object_id, 
           |       guide_image_weights.description as object_description, weight 
           |FROM guide_images JOIN guide_image_weights ON guide_images.id = guide_image_id 
           |WHERE guide_images.id = {id} ORDER BY guide_image_weights.object_id""".stripMargin

      val result = SQL(query).on('id -> id).executeQuery().as(Macro.namedParser[GuideResultRow].*)

      if (result.isEmpty)
        Left(ResourceError.ResourceNotFound)
      else {
        val weights = result.map(row => GuideImageWeightRecord(row.object_description, row.object_id, row.weight))

        Right(GuideImage(id, result.head.image_description, weights))
      }
  }

  case class DrinkwareResultRow(id: String, scale_id: Long, description: String, guide_image_id: String,
    width: Int, height: Int, empty_level: Int, full_level: Int, choice_id: Int, base_image_url: String,
    overlay_image_url: String)

  case class VolumeSampleResultRow(scale_id: Long, fill: Double, volume: Double)

  def drinkwareDef(id: String): Either[ResourceError, DrinkwareSet] = tryWithConnection {
    implicit conn =>
      val drinkwareScalesQuery =
        """|SELECT drinkware_sets.id, drinkware_scales.id as scale_id, description, guide_image_id, 
         |       width, height, empty_level, full_level, choice_id, base_image_url, overlay_image_url
         |FROM drinkware_sets JOIN drinkware_scales ON drinkware_set_id = drinkware_sets.id
         |WHERE drinkware_sets.id = {drinkware_id}
         |ORDER by scale_id""".stripMargin

      val result = SQL(drinkwareScalesQuery).on('drinkware_id -> id).executeQuery().as(Macro.namedParser[DrinkwareResultRow].*)

      if (result.isEmpty)
        Left(ResourceError.ResourceNotFound)
      else {
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
}
