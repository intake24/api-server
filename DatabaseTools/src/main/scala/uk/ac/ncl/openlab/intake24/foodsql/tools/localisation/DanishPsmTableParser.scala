package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.UserFoodHeader

class DanishPsmTableParser extends PortionSizeTableParser {

  def parsePortionSizeMethodsTable(csvPath: String, nutrientTableCsvPath: String, localToIntakeCodes: Map[String, String], indexableFoods: Seq[UserFoodHeader],
    dataService: FoodDatabaseAdminService): Map[String, Seq[PortionSizeMethod]] = Map()

}