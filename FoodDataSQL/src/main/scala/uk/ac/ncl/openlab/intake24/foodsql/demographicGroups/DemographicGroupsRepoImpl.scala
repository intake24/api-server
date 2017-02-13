package uk.ac.ncl.openlab.intake24.foodsql.demographicGroups

import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._

import anorm._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}

/**
  * Created by Tim Osadchiy on 09/02/2017.
  */

@Singleton
class DemographicGroupsServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource)
  extends DemographicGroupsService with FoodDataSqlService {

  private object DemographicGroupConstants {
    final val rangeStep = 0.001
  }

  private case class DemographicGroupWithScaleDbQueryRow(id: Long,
                                                         age_start: Option[Int],
                                                         age_end: Option[Int],
                                                         weight_start: Option[Double],
                                                         weight_end: Option[Double],
                                                         height_start: Option[Double],
                                                         height_end: Option[Double],
                                                         sex: Option[String],
                                                         physical_activity_level_id: Option[Long],
                                                         nutrient_type_id: Option[Long],
                                                         sector_id: Option[Long],
                                                         sector_start: Option[Double],
                                                         sector_end: Option[Double],
                                                         sentiment: String,
                                                         sector_name: String,
                                                         sector_description: Option[String]
                                                        ) {

    def toDemographicGroupRecord(scaleSectors: Seq[DemographicScaleSectorRecord]) = new DemographicGroupRecord(
      this.id,
      this.sex,
      for (start <- this.age_start; end <- this.age_end) yield IntRange(start, end),
      for (start <- this.height_start; end <- this.height_end) yield DoubleRange(start, end),
      for (start <- this.weight_start; end <- this.weight_end) yield DoubleRange(start, end),
      this.physical_activity_level_id,
      scaleSectors
    )

    def toScaleSectorRecord() = new DemographicScaleSectorRecord(
      this.sector_id.get,
      this.sector_name,
      this.sector_description,
      this.sentiment,
      Range.Double(this.sector_start.get, this.sector_end.get, DemographicGroupConstants.rangeStep)
    )

  }

  private case class DemographicGroupDbQueryRow(id: Long,
                                                sex: Option[String],
                                                age_start: Option[Int],
                                                age_end: Option[Int],
                                                height_start: Option[Double],
                                                height_end: Option[Double],
                                                weight_start: Option[Double],
                                                weight_end: Option[Double],
                                                physical_activity_level_id: Option[Long]
                                               )

  private case class DemographicGroupsScaleSectorDbRow(id: Long, name: String, description: Option[String],
                                                       sentiment: String, range_start: Double, range_end: Double)

  def list(): Either[UnexpectedDatabaseError, Seq[DemographicGroupRecord]] = tryWithConnection {
    implicit conn =>
      val sqlQuery =
        """
          |SELECT dg.id,
          |       lower(dg.age) as age_start,
          |       upper(dg.age) as age_end,
          |       lower(dg.weight) as weight_start,
          |       upper(dg.weight) as weight_end,
          |       lower(dg.height) as height_start,
          |       upper(dg.height) as height_end,
          |       dg.sex,
          |       dg.physical_activity_level_id,
          |       dg.nutrient_type_id,
          |       dgs.id AS sector_id,
          |       lower(dgs.range) as sector_start,
          |       upper(dgs.range) as sector_end,
          |       dgs.sentiment,
          |       dgs.name as sector_name,
          |       dgs.description as sector_description
          |FROM demographic_group AS dg
          |JOIN demographic_group_scale_sector AS dgs ON dgs.demographic_group_id = dg.id;
        """.stripMargin

      val rows = SQL(sqlQuery).executeQuery().as(Macro.namedParser[DemographicGroupWithScaleDbQueryRow].*)

      val result = rows.groupBy(_.id).map({
        case (key, records) =>
          val scaleSectors = records.map(childRec =>
            childRec.toScaleSectorRecord()
          )
          records(0).toDemographicGroupRecord(scaleSectors)
      }).toList

      Right(result)


  }

}
