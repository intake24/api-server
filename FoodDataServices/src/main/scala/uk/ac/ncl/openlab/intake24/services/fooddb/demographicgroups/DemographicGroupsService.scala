package uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups

import uk.ac.ncl.openlab.intake24.services.fooddb.errors._

import scala.collection.immutable.NumericRange

case class DoubleRange(start: Double, end: Double)

case class IntRange(start: Int, end: Int)

case class DemographicScaleSectorRecord(id: Long,
                                        name: String,
                                        description: Option[String],
                                        sentiment: String,
                                        range: NumericRange[Double])

case class DemographicGroupRecord(id: Long,
                                  sex: Option[String],
                                  age: Option[IntRange],
                                  height: Option[DoubleRange],
                                  weight: Option[DoubleRange],
                                  physicalLevelId: Option[Long],
                                  scaleSectors: Seq[DemographicScaleSectorRecord]
                                 )

trait DemographicGroupsService {

  def list(): Either[UnexpectedDatabaseError, Seq[DemographicGroupRecord]]

}