package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.sql.DataSource

import anorm.{SQL, SqlParser, sqlToSimple, ~}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.NdnsCompoundFoodGroupsService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService


@Singleton
class NdnsCompoundFoodGroupsImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends NdnsCompoundFoodGroupsService with SqlDataService {

  private val logger = LoggerFactory.getLogger(classOf[NdnsCompoundFoodGroupsImpl])

  override def getCompoundFoodGroupsData(ndnsCodes: Set[Int]): Either[UnexpectedDatabaseError, Map[Int, Map[Int, Double]]] = tryWithConnection {
    implicit conn =>
      Right(SQL(
        """SELECT ndns_food_code AS f, compound_food_group_id AS g, proportion FROM ndns_compound_food_groups_data
          | WHERE ndns_food_code IN ({ndnsCodes})""".stripMargin)
        .on('ndnsCodes -> ndnsCodes)
        .as((SqlParser.int(1) ~ SqlParser.int(2) ~ SqlParser.double(3)).*)
        .foldLeft(Map[Int, Map[Int, Double]]().withDefaultValue(Map())) {
          case (acc, food_code ~ food_group_id ~ proportion) =>
            acc + (food_code -> (acc(food_code) + (food_group_id -> proportion)))
        })
  }
}
