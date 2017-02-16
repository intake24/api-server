package uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups

import uk.ac.ncl.openlab.intake24.errors._

import scala.collection.immutable.NumericRange

abstract class NumRange[T] {
  val start: T
  val end: T
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
                                     scaleSectors: Seq[DemographicScaleSectorOut]
                                    )

case class DemographicGroupRecordIn(sex: Option[String],
                                    age: Option[IntRange],
                                    height: Option[DoubleRange],
                                    weight: Option[DoubleRange],
                                    physicalLevelId: Option[Long],
                                    nutrientTypeId: Long
                                   )

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