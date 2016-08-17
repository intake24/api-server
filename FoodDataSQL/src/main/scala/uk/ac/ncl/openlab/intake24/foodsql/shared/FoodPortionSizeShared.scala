package uk.ac.ncl.openlab.intake24.foodsql.shared

import anorm.Macro
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import anorm.SQL
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation

trait FoodPortionSizeShared extends SqlResourceLoader with FirstRowValidation {

  protected case class PsmResultRow(id: Long, method: String, description: String, image_url: String, use_for_recipes: Boolean, param_name: Option[String], param_value: Option[String])

  protected val psmResultRowParser = Macro.namedParser[PsmResultRow]

  protected def mkPortionSizeMethods(rows: Seq[PsmResultRow]) =
    // FIXME: surely there is a better method to group records preserving order...
    rows.groupBy(_.id).toSeq.sortBy(_._1).map {
      case (id, rows) =>
        val params = rows.filterNot(r => r.param_name.isEmpty || r.param_value.isEmpty).map(row => PortionSizeMethodParameter(row.param_name.get, row.param_value.get))
        val head = rows.head

        PortionSizeMethod(head.method, head.description, head.image_url, head.use_for_recipes, params)
    }
  
  private lazy val foodPsmQuery = sqlFromResource("shared/food_portion_size_methods.sql")

  protected def foodPortionSizeMethodsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[PortionSizeMethod]] = {
    val psmResults = SQL(foodPsmQuery).on('food_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(psmResults, psmResultRowParser.+)(Seq(FirstRowValidationClause("id", Right(List())))).right.map(mkPortionSizeMethods)
  }
}
