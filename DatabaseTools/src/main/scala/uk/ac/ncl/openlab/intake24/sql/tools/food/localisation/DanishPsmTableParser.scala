package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import uk.ac.ncl.openlab.intake24.api.data.{PortionSizeMethod, UserFoodHeader}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService

class DanishPsmTableParser extends PortionSizeTableParser {

  def parsePortionSizeMethodsTable(csvPath: String, nutrientTableCsvPath: String, localToIntakeCodes: Map[String, String], indexableFoods: Seq[UserFoodHeader],
                                   dataService: FoodsAdminService): Map[String, Seq[PortionSizeMethod]] = Map()

}