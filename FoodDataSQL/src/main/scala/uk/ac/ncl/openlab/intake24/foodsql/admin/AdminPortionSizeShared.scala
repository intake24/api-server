package uk.ac.ncl.openlab.intake24.foodsql.admin

import anorm.Macro
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.PortionSizeMethod

trait AdminPortionSizeShared {

  protected case class PsmResultRow(id: Long, method: String, description: String, image_url: String, use_for_recipes: Boolean, param_name: Option[String], param_value: Option[String])

  protected case class RecursivePsmResultRow(id: Long, category_code: String, method: String, description: String, image_url: String, use_for_recipes: Boolean, param_name: Option[String], param_value: Option[String])

  protected case class RecursiveAttributesRow(same_as_before_option: Option[Boolean], ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  protected val psmResultRowParser = Macro.namedParser[PsmResultRow]

  protected val recursivePsmResultRowParser = Macro.namedParser[RecursivePsmResultRow]

  protected val recursiveAttributesRowParser = Macro.namedParser[RecursiveAttributesRow]

  protected def mkPortionSizeMethods(rows: Seq[PsmResultRow]) =
    // FIXME: surely there is a better method to group records preserving order...
    rows.groupBy(_.id).toSeq.sortBy(_._1).map {
      case (id, rows) =>
        val params = rows.filterNot(r => r.param_name.isEmpty || r.param_value.isEmpty).map(row => PortionSizeMethodParameter(row.param_name.get, row.param_value.get))
        val head = rows.head

        PortionSizeMethod(head.method, head.description, head.image_url, head.use_for_recipes, params)
    }

  protected def mkRecursivePortionSizeMethods(rows: Seq[RecursivePsmResultRow]) =
    if (rows.isEmpty)
      Seq()
    else {
      val firstCategoryCode = rows.head.category_code
      mkPortionSizeMethods(rows.takeWhile(_.category_code == firstCategoryCode).map(r => PsmResultRow(r.id, r.method, r.description, r.image_url, r.use_for_recipes, r.param_name, r.param_value)))
    }
}
