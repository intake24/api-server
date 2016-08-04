package uk.ac.ncl.openlab.intake24.foodsql.admin

trait AdminErrorMessagesShared {

  protected val foodCodeFkConstraintFailedCode = "invalid_food_code"

  protected val localeFkConstraintFailedCode = "invalid_locale_code"

  protected def foodCodeFkConstraintFailedMessage(foodCode: String) =
    s"Food code $foodCode is not defined. Either an invalid code was supplied or the food was deleted or had its code changed by someone else."

  protected def localeFkConstraintFailedMessage(locale: String) =
    s"Locale $locale is not defined."

}