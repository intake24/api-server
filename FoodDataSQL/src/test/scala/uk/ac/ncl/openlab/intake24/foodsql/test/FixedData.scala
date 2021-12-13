package uk.ac.ncl.openlab.intake24.foodsql.test

import uk.ac.ncl.openlab.intake24.Locale

trait FixedData {
  val testLocale = Locale("locale1", "Locale 1", "Великая локаль 1", "en", "en", "gb", None, "ltr")

  val undefinedLocaleId = "undefined_locale"
}
