package uk.ac.ncl.openlab.intake24.foodsql.test

import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter

trait FixedData {
  val testLocale = Locale("locale1", "Locale 1", "Великая локаль 1", "en", "en", "gb", None, "ltr")

  val undefinedLocaleId = "undefined_locale"
}