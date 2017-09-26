package uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups

import uk.ac.ncl.openlab.intake24.errors._

abstract class NumRange[Numeric] {
  val start: Numeric
  val end: Numeric
}

case class DoubleRange(override val start: Double, override val end: Double) extends NumRange[Double]

case class IntRange(override val start: Int, override val end: Int) extends NumRange[Int]

case class DemographicScaleSectorOut(id: Long,
                                     name: String,
                                     description: Option[String],
                                     sentiment: String,
                                     range: DoubleRange)

case class DemographicScaleSectorIn(name: String,
                                    description: Option[String],
                                    sentiment: String,
                                    range: DoubleRange)

case class DemographicGroupRecordOut(id: Long,
                                     sex: Option[String],
                                     age: Option[IntRange],
                                     height: Option[DoubleRange],
                                     weight: Option[DoubleRange],
                                     physicalLevelId: Option[Long],
                                     nutrientTypeId: Long,
                                     nutrientRuleType: String,
                                     nutrientTypeKCalPerUnit: Option[Double],
                                     scaleSectors: Seq[DemographicScaleSectorOut]
                                    )

case class DemographicGroupRecordIn(sex: Option[String],
                                    age: Option[IntRange],
                                    height: Option[DoubleRange],
                                    weight: Option[DoubleRange],
                                    physicalLevelId: Option[Long],
                                    nutrientTypeId: Long,
                                    nutrientRuleType: String,
                                    nutrientTypeKCalPerUnit: Option[Double]
                                   )

object DemographicGroupRecord {
  val NUTRIENT_RULE_TYPE_PERCENTAGE_OF_ENERGY = "percentage_of_energy"
  val NUTRIENT_RULE_TYPE_ENERGY_DIVIDED_BY_BMR = "energy_divided_by_bmr"
  val NUTRIENT_RULE_TYPE_PER_UNIT_OF_WEIGHT = "per_unit_of_weight"
  val NUTRIENT_RULE_TYPE_RANGE = "range"

  def isValid(demographicGroupRecordIn: DemographicGroupRecordIn): Either[ConstraintError, Unit] = {
    demographicGroupRecordIn.nutrientRuleType match {
      case NUTRIENT_RULE_TYPE_PERCENTAGE_OF_ENERGY =>
        demographicGroupRecordIn.nutrientTypeKCalPerUnit match {
          case None =>
            Left(new ConstraintViolation("nutrient_type_kcal_per_unit_not_defined",
              new RuntimeException(s"${demographicGroupRecordIn.nutrientRuleType} requires nutrientTypeKCalPerUnit " +
                s"being defined")))
          case Some(v) =>
            Right(())
        }
      case NUTRIENT_RULE_TYPE_ENERGY_DIVIDED_BY_BMR | NUTRIENT_RULE_TYPE_RANGE | NUTRIENT_RULE_TYPE_PER_UNIT_OF_WEIGHT =>
        Right(())
      case _ =>
        Left(new ConstraintViolation("nutrient_rule_type_invalid",
          new RuntimeException("nutrientRuleType is not valid")))
    }
  }

}

trait DemographicGroupsService {

  def list(): Either[UnexpectedDatabaseError, Seq[DemographicGroupRecordOut]]

  def createDemographicGroup(demographicRecord: DemographicGroupRecordIn): Either[ConstraintError, DemographicGroupRecordOut]

  def patchDemographicGroup(id: Int, demographicRecord: DemographicGroupRecordIn): Either[UpdateError, DemographicGroupRecordOut]

  def getDemographicGroup(id: Int): Either[LookupError, DemographicGroupRecordOut]

  def deleteDemographicGroup(id: Int): Either[UnexpectedDatabaseError, Unit]

  def createDemographicScaleSector(demographicGroupId: Int, demographicRecord: DemographicScaleSectorIn): Either[UpdateError, DemographicScaleSectorOut]

  def patchDemographicScaleSector(id: Int, demographicRecord: DemographicScaleSectorIn): Either[UpdateError, DemographicScaleSectorOut]

  def deleteDemographicScaleSector(id: Int): Either[UnexpectedDatabaseError, Unit]

  def getDemographicScaleSectorSentimentTypes(): Seq[String]

}