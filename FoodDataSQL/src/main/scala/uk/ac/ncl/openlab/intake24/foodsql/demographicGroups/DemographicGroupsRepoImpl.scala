package uk.ac.ncl.openlab.intake24.foodsql.demographicGroups

import javax.sql.DataSource

import anorm._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

/**
  * Created by Tim Osadchiy on 09/02/2017.
  */

@Singleton
class DemographicGroupsServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource)
  extends DemographicGroupsService with SqlDataService {

  val constraintErrorsPartialFn: PartialFunction[String, PSQLException => ConstraintError] = {
    case constraintName => (e: PSQLException) => ConstraintViolation(constraintName, e)
  }

  private def unpackOptionalRangeToQuery[T](range: Option[NumRange[T]]): Option[String] = {
    range match {
      case None =>
        Option.empty[String]
      case Some(null) =>
        Option.empty[String]
      case Some(r) =>
        range.map(r => s"[${r.start}, ${r.end})")
    }
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
                                                         nutrient_type_id: Long,
                                                         nutrient_rule_type: String,
                                                         nutrient_type_kcal_per_unit: Option[Double],
                                                         sector_id: Option[Long],
                                                         sector_start: Option[Double],
                                                         sector_end: Option[Double],
                                                         sentiment: Option[String],
                                                         sector_name: Option[String],
                                                         sector_description: Option[String]
                                                        ) {

    def toDemographicGroupRecord(scaleSectors: Seq[DemographicScaleSectorOut]) = new DemographicGroupRecordOut(
      this.id,
      this.sex,
      for (start <- this.age_start; end <- this.age_end) yield IntRange(start, end),
      for (start <- this.height_start; end <- this.height_end) yield DoubleRange(start, end),
      for (start <- this.weight_start; end <- this.weight_end) yield DoubleRange(start, end),
      this.physical_activity_level_id,
      this.nutrient_type_id,
      this.nutrient_rule_type,
      this.nutrient_type_kcal_per_unit,
      scaleSectors
    )

    def toScaleSectorRecord(): Option[DemographicScaleSectorOut] = for (
      id <- this.sector_id;
      name <- this.sector_name;
      sentiment <- this.sentiment;
      start <- this.sector_start;
      end <- this.sector_end
    ) yield DemographicScaleSectorOut(
      id,
      name,
      this.sector_description,
      sentiment,
      DoubleRange(start, end)
    )


  }

  private case class DemographicGroupDbQueryRow(id: Long,
                                                age_start: Option[Int],
                                                age_end: Option[Int],
                                                weight_start: Option[Double],
                                                weight_end: Option[Double],
                                                height_start: Option[Double],
                                                height_end: Option[Double],
                                                sex: Option[String],
                                                physical_activity_level_id: Option[Long],
                                                nutrient_type_id: Long,
                                                nutrient_rule_type: String,
                                                nutrient_type_kcal_per_unit: Option[Double]
                                               ) {

    def toDemographicGroupRecord() = new DemographicGroupRecordOut(
      this.id,
      this.sex,
      for (start <- this.age_start; end <- this.age_end) yield IntRange(start, end),
      for (start <- this.height_start; end <- this.height_end) yield DoubleRange(start, end),
      for (start <- this.weight_start; end <- this.weight_end) yield DoubleRange(start, end),
      this.physical_activity_level_id,
      this.nutrient_type_id,
      this.nutrient_rule_type,
      this.nutrient_type_kcal_per_unit,
      Seq()
    )

  }

  private object DemographicGroupDbQueryRow {

    private def unpackOptionalRange[T](range: Option[NumRange[T]]): (Option[T], Option[T]) = {
      range match {
        case Some(r) =>
          (Some(r.start), Some(r.end))
        case None =>
          (Option.empty[T], Option.empty[T])
      }
    }

    def fromRecord(record: DemographicGroupRecordOut): DemographicGroupDbQueryRow = {
      DemographicGroupDbQueryRow(
        record.id,
        unpackOptionalRange[Int](record.age)._1,
        unpackOptionalRange[Int](record.age)._2,
        unpackOptionalRange[Double](record.height)._1,
        unpackOptionalRange[Double](record.height)._2,
        unpackOptionalRange[Double](record.weight)._1,
        unpackOptionalRange[Double](record.weight)._2,
        record.sex,
        record.physicalLevelId,
        record.nutrientTypeId,
        record.nutrientRuleType,
        record.nutrientTypeKCalPerUnit
      )
    }

    def getSqlInsertFromRecord(record: DemographicGroupRecordIn): SimpleSql[Row] = {
      val age_str = unpackOptionalRangeToQuery[Int](record.age)
      val height_str = unpackOptionalRangeToQuery[Double](record.height)
      val weight_str = unpackOptionalRangeToQuery[Double](record.weight)

      val dgInsertQuery =
        """
          |INSERT INTO demographic_group (
          |         age,
          |         weight,
          |         height,
          |         sex,
          |         physical_activity_level_id,
          |         nutrient_type_id,
          |         nutrient_rule_type )
          |VALUES ( {age}::int4range,
          |         {weight}::numrange,
          |         {height}::numrange,
          |         {sex}::sex_enum,
          |         {physical_activity_level_id},
          |         {nutrient_type_id},
          |         {nutrient_rule_type}::nutrient_rule_type_enum)
          |RETURNING id,
          |          lower(age) as age_start,
          |          upper(age) as age_end,
          |          lower(weight) as weight_start,
          |          upper(weight) as weight_end,
          |          lower(height) as height_start,
          |          upper(height) as height_end,
          |          sex,
          |          physical_activity_level_id,
          |          nutrient_type_id,
          |          nutrient_rule_type,
          |          Null as nutrient_type_kcal_per_unit
          |""".stripMargin

      record.nutrientTypeKCalPerUnit match {
        case Some(v) =>
          val bigQuery =
            s"""
               |WITH dg_ins AS (
               |   ${dgInsertQuery}
               |), kcal_ins AS (
               |   INSERT INTO nutrient_type_in_kcal (nutrient_type_id, kcal_per_unit)
               |   VALUES ({nutrient_type_id}, {nutrient_type_kcal_per_unit})
               |   ON CONFLICT (nutrient_type_id) DO UPDATE
               |   SET kcal_per_unit = {nutrient_type_kcal_per_unit}
               |   RETURNING id, nutrient_type_id, kcal_per_unit
               |)
               |SELECT dg_ins.id,
               |       dg_ins.age_start,
               |       dg_ins.age_end,
               |       dg_ins.weight_start,
               |       dg_ins.weight_end,
               |       dg_ins.height_start,
               |       dg_ins.height_end,
               |       dg_ins.sex,
               |       dg_ins.physical_activity_level_id,
               |       dg_ins.nutrient_type_id,
               |       dg_ins.nutrient_rule_type,
               |       kcal_ins.kcal_per_unit as nutrient_type_kcal_per_unit
               |FROM dg_ins
               |JOIN kcal_ins
               |ON kcal_ins.nutrient_type_id = dg_ins.nutrient_type_id;
            """.stripMargin
          SQL(bigQuery).on(
            'age -> age_str,
            'weight -> weight_str,
            'height -> height_str,
            'sex -> record.sex,
            'physical_activity_level_id -> record.physicalLevelId,
            'nutrient_type_id -> record.nutrientTypeId,
            'nutrient_rule_type -> record.nutrientRuleType,
            'nutrient_type_kcal_per_unit -> v)
        case None =>
          SQL(dgInsertQuery).on(
            'age -> age_str,
            'weight -> weight_str,
            'height -> height_str,
            'sex -> record.sex,
            'physical_activity_level_id -> record.physicalLevelId,
            'nutrient_type_id -> record.nutrientTypeId,
            'nutrient_rule_type -> record.nutrientRuleType)
      }
    }

    def getSqlUpdateFromRecord(id: Int, record: DemographicGroupRecordIn): SimpleSql[Row] = {
      val age_str = unpackOptionalRangeToQuery[Int](record.age)
      val height_str = unpackOptionalRangeToQuery[Double](record.height)
      val weight_str = unpackOptionalRangeToQuery[Double](record.weight)

      val dgUpdateQuery =
        """
          |UPDATE demographic_group
          |SET age = {age}::int4range,
          |    weight = {weight}::numrange,
          |    height = {height}::numrange,
          |    sex = {sex}::sex_enum,
          |    physical_activity_level_id = {physical_activity_level_id},
          |    nutrient_type_id = {nutrient_type_id},
          |    nutrient_rule_type = {nutrient_rule_type}::nutrient_rule_type_enum
          |WHERE id = {id}
          |RETURNING id,
          |          lower(age) as age_start,
          |          upper(age) as age_end,
          |          lower(weight) as weight_start,
          |          upper(weight) as weight_end,
          |          lower(height) as height_start,
          |          upper(height) as height_end,
          |          sex,
          |          physical_activity_level_id,
          |          nutrient_type_id,
          |          nutrient_rule_type,
          |          Null as nutrient_type_kcal_per_unit
          |
          |""".stripMargin

      record.nutrientTypeKCalPerUnit match {
        case Some(v) =>
          val bigQuery =
            s"""
               |WITH dg_update AS (
               |   ${dgUpdateQuery}
               |), kcal_update AS (
               |   INSERT INTO nutrient_type_in_kcal (nutrient_type_id, kcal_per_unit)
               |   VALUES ({nutrient_type_id}, {nutrient_type_kcal_per_unit})
               |   ON CONFLICT (nutrient_type_id) DO UPDATE
               |   SET kcal_per_unit = {nutrient_type_kcal_per_unit}
               |   RETURNING id, nutrient_type_id, kcal_per_unit
               |)
               |SELECT dg_update.id,
               |       dg_update.age_start,
               |       dg_update.age_end,
               |       dg_update.weight_start,
               |       dg_update.weight_end,
               |       dg_update.height_start,
               |       dg_update.height_end,
               |       dg_update.sex,
               |       dg_update.physical_activity_level_id,
               |       dg_update.nutrient_type_id,
               |       dg_update.nutrient_rule_type,
               |       kcal_update.kcal_per_unit as nutrient_type_kcal_per_unit
               |FROM dg_update
               |JOIN kcal_update
               |ON kcal_update.nutrient_type_id = dg_update.nutrient_type_id;
            """.stripMargin
          SQL(bigQuery).on(
            'id -> id,
            'age -> age_str,
            'weight -> weight_str,
            'height -> height_str,
            'sex -> record.sex,
            'physical_activity_level_id -> record.physicalLevelId,
            'nutrient_type_id -> record.nutrientTypeId,
            'nutrient_rule_type -> record.nutrientRuleType,
            'nutrient_type_kcal_per_unit -> v)
        case None =>
          SQL(dgUpdateQuery).on(
            'id -> id,
            'age -> age_str,
            'weight -> weight_str,
            'height -> height_str,
            'sex -> record.sex,
            'physical_activity_level_id -> record.physicalLevelId,
            'nutrient_type_id -> record.nutrientTypeId,
            'nutrient_rule_type -> record.nutrientRuleType)
      }
    }

    def getSqlGet(id: Int): SimpleSql[Row] = {
      val query =
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
          |       dg.nutrient_rule_type,
          |       nt_kcal.kcal_per_unit as nutrient_type_kcal_per_unit,
          |       dgs.id AS sector_id,
          |       lower(dgs.range) as sector_start,
          |       upper(dgs.range) as sector_end,
          |       dgs.sentiment,
          |       dgs.name as sector_name,
          |       dgs.description as sector_description
          |FROM demographic_group AS dg
          |LEFT JOIN nutrient_type_in_kcal AS nt_kcal ON nt_kcal.nutrient_type_id = dg.nutrient_type_id
          |LEFT JOIN demographic_group_scale_sector AS dgs ON dgs.demographic_group_id = dg.id
          |WHERE dg.id = {id};
        """.stripMargin

      SQL(query).on('id -> id)
    }

    def getSqlDelete(id: Int): SimpleSql[Row] = {
      val query =
        """
          |DELETE FROM demographic_group
          |WHERE id = {id};
        """.stripMargin

      SQL(query).on('id -> id)
    }

  }

  private case class DemographicGroupsScaleSectorDbQueryRow(id: Long,
                                                            name: String,
                                                            description: Option[String],
                                                            sentiment: String,
                                                            range_start: Double,
                                                            range_end: Double) {
    def toRecord() = new DemographicScaleSectorOut(
      this.id,
      this.name,
      this.description,
      this.sentiment,
      DoubleRange(start = this.range_start, end = this.range_end)
    )
  }

  private object DemographicGroupsScaleSectorDbQueryRow {

    def getSqlInsertFromRecord(demographicGroupId: Int, record: DemographicScaleSectorIn): SimpleSql[Row] = {
      val query =
        """
          |INSERT INTO demographic_group_scale_sector
          |       (demographic_group_id,
          |        name,
          |        description,
          |        sentiment,
          |        range)
          |VALUES ( {demographic_group_id},
          |         {name},
          |         {description},
          |         {sentiment}::sentiment_enum,
          |         {range}::numrange
          |) RETURNING id,
          |            name,
          |            description,
          |            sentiment,
          |            lower(range) as range_start,
          |            upper(range) as range_end
          |""".stripMargin

      SQL(query).on(
        'demographic_group_id -> demographicGroupId,
        'name -> record.name,
        'description -> record.description,
        'sentiment -> record.sentiment,
        'range -> s"[${record.range.start}, ${record.range.end})"
      )
    }

    def getSqlUpdateFromRecord(id: Int, record: DemographicScaleSectorIn): SimpleSql[Row] = {

      val query =
        """
          |UPDATE demographic_group_scale_sector
          |SET name = {name},
          |    description = {description},
          |    sentiment = {sentiment}::sentiment_enum,
          |    range = {range}::numrange
          |WHERE id = {id}
          |RETURNING id,
          |          name,
          |          description,
          |          sentiment,
          |          lower(range) as range_start,
          |          upper(range) as range_end
          |""".stripMargin

      SQL(query).on(
        'id -> id,
        'name -> record.name,
        'description -> record.description,
        'sentiment -> record.sentiment,
        'range -> s"[${record.range.start}, ${record.range.end})"
      )
    }

    def getSqlDelete(id: Int): SimpleSql[Row] = {
      val query =
        """
          |DELETE
          |FROM demographic_group_scale_sector
          |WHERE id = {id};
        """.stripMargin

      SQL(query).on('id -> id)
    }

  }

  def list(): Either[UnexpectedDatabaseError, Seq[DemographicGroupRecordOut]] = tryWithConnection {
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
          |       dg.nutrient_rule_type,
          |       nt_kcal.kcal_per_unit as nutrient_type_kcal_per_unit,
          |       dgs.id AS sector_id,
          |       lower(dgs.range) as sector_start,
          |       upper(dgs.range) as sector_end,
          |       dgs.sentiment,
          |       dgs.name as sector_name,
          |       dgs.description as sector_description
          |FROM demographic_group AS dg
          |LEFT JOIN nutrient_type_in_kcal AS nt_kcal ON nt_kcal.nutrient_type_id = dg.nutrient_type_id
          |LEFT JOIN demographic_group_scale_sector AS dgs ON dgs.demographic_group_id = dg.id;
        """.stripMargin

      val rows = SQL(sqlQuery).executeQuery().as(Macro.namedParser[DemographicGroupWithScaleDbQueryRow].*)

      val result = rows.groupBy(_.id).map({
        case (key, records) =>
          val scaleSectors = records.flatMap(childRec =>
            childRec.toScaleSectorRecord()
          )
          records.head.toDemographicGroupRecord(scaleSectors)
      }).toList

      Right(result)


  }

  def createDemographicGroup(demographicRecord: DemographicGroupRecordIn): Either[ConstraintError, DemographicGroupRecordOut] = tryWithConnection {
    implicit conn =>

      DemographicGroupRecord.isValid(demographicRecord) match {
        case Left(e) =>
          Left(e)
        case _ =>
          tryWithConstraintsCheck[ConstraintError, DemographicGroupRecordOut](constraintErrorsPartialFn) {
            val rows = DemographicGroupDbQueryRow.getSqlInsertFromRecord(demographicRecord).executeQuery()
              .as(Macro.namedParser[DemographicGroupDbQueryRow].single)
            Right(rows.toDemographicGroupRecord())
          }
      }

  }

  override def patchDemographicGroup(id: Int, demographicRecord: DemographicGroupRecordIn): Either[UpdateError, DemographicGroupRecordOut] = tryWithConnection {
    implicit conn =>

      DemographicGroupRecord.isValid(demographicRecord) match {
        case Left(e) =>
          Left(e)
        case _ =>
          tryWithConstraintsCheck[UpdateError, DemographicGroupRecordOut](constraintErrorsPartialFn) {
            DemographicGroupDbQueryRow.getSqlUpdateFromRecord(id, demographicRecord).executeQuery()
              .as(Macro.namedParser[DemographicGroupDbQueryRow].singleOpt) match {
              case Some(row) =>
                Right(row.toDemographicGroupRecord())
              case None =>
                Left(RecordNotFound(new RuntimeException(s"Demographic group set $id not found")))
            }
          }
      }
  }

  override def getDemographicGroup(id: Int): Either[LookupError, DemographicGroupRecordOut] = tryWithConnection {
    implicit conn =>
      val res = DemographicGroupDbQueryRow.getSqlGet(id).executeQuery().as(Macro.namedParser[DemographicGroupWithScaleDbQueryRow].*)
      res match {
        case Nil =>
          Left(RecordNotFound(new RuntimeException(s"Demographic group set $id not found")))
        case rows =>
          val result = rows.groupBy(_.id).map({
            case (key, records) =>
              val scaleSectors = records.flatMap(childRec =>
                childRec.toScaleSectorRecord()
              )
              records.head.toDemographicGroupRecord(scaleSectors)
          }).toList.head
          Right(result)
      }

  }

  override def deleteDemographicGroup(id: Int): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      DemographicGroupDbQueryRow.getSqlDelete(id).execute()
      Right(())
  }

  override def createDemographicScaleSector(demographicGroupId: Int,
                                            sectorRecord: DemographicScaleSectorIn):
  Either[ConstraintError, DemographicScaleSectorOut] = tryWithConnection {

    implicit conn =>

      tryWithConstraintsCheck[ConstraintError, DemographicScaleSectorOut](constraintErrorsPartialFn) {
        val rows = DemographicGroupsScaleSectorDbQueryRow.getSqlInsertFromRecord(demographicGroupId,
          sectorRecord).executeQuery().as(Macro.namedParser[DemographicGroupsScaleSectorDbQueryRow].single)
        Right(rows.toRecord())
      }
  }

  override def patchDemographicScaleSector(id: Int, sectorRecord: DemographicScaleSectorIn): Either[UpdateError, DemographicScaleSectorOut] = tryWithConnection {
    implicit conn =>

      tryWithConstraintsCheck[UpdateError, DemographicScaleSectorOut](constraintErrorsPartialFn) {
        DemographicGroupsScaleSectorDbQueryRow.getSqlUpdateFromRecord(id, sectorRecord).executeQuery()
          .as(Macro.namedParser[DemographicGroupsScaleSectorDbQueryRow].singleOpt) match {
          case None =>
            Left(RecordNotFound(new RuntimeException(s"Demographic group set $id not found")))
          case Some(result) =>
            Right(result.toRecord())
        }
      }

  }

  override def deleteDemographicScaleSector(id: Int): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      DemographicGroupsScaleSectorDbQueryRow.getSqlDelete(id).execute()
      Right(())
  }

  override def getDemographicScaleSectorSentimentTypes(): Seq[String] = {
    Seq("highly_negative",
      "negative",
      "warning",
      "neutral",
      "positive",
      "highly_positive"
    )
  }

}
