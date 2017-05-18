package uk.ac.ncl.openlab.intake24.foodsql.demographicGroups

import javax.sql.DataSource

import anorm.{Macro, SQL}
import com.google.inject.Inject
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups.{DemographicGroupRecordOut, DemographicGroupsService, PhysicalActivityLevelOut, PhysicalActivityLevelService}
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

/**
  * Created by Tim Osadchiy on 18/05/2017.
  */
class PhysicalActivityLevelImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource)
  extends PhysicalActivityLevelService with SqlDataService {

  def list(): Either[UnexpectedDatabaseError, Seq[PhysicalActivityLevelOut]] = tryWithConnection {
    implicit conn =>
      val sqlQuery =
        """
          |SELECT id, name, coefficient
          |FROM physical_activity_levels;
        """.stripMargin

      val rows = SQL(sqlQuery).executeQuery().as(Macro.namedParser[PhysicalActivityLevelOut].*)

      Right(rows)


  }

}
