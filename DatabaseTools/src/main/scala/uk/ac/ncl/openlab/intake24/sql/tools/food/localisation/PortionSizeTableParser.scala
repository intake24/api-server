package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.UserFoodHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService

trait PortionSizeTableParser {
  def parsePortionSizeMethodsTable(csvPath: String, nutrientTableCsvPath: String, localToIntakeCodes: Map[String, String], indexableFoods: Seq[UserFoodHeader],
    dataService: FoodDatabaseAdminService): Map[String, Seq[PortionSizeMethod]]
}