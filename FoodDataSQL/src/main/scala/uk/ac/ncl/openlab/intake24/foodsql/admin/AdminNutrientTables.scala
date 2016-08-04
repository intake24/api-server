package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.NutrientTable
import anorm.SQL
import anorm.Macro
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService

trait AdminNutrientTables extends SqlDataService {
  private case class NutrientTableDescRow(id: String, description: String) {
    def asNutrientTable = NutrientTable(id, description)
  }

  def nutrientTables(): Seq[NutrientTable] = tryWithConnection {
    implicit conn =>
      val query = """SELECT id, description FROM nutrient_tables ORDER BY id ASC"""
      SQL(query).executeQuery().as(Macro.namedParser[NutrientTableDescRow].*).map(_.asNutrientTable)
  }
}
