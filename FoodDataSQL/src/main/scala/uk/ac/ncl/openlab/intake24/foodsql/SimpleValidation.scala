package uk.ac.ncl.openlab.intake24.foodsql

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import anorm.SQL
import anorm.Macro
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import anorm.SqlParser

trait SimpleValidation {

  private val foodAndLocaleValidationQuery = "SELECT (SELECT true FROM foods WHERE code={food_code}) AS food_exists, (SELECT true FROM locales WHERE id={locale_id}) as locale_exists"

  private val localeValidationQuery = "SELECT (SELECT true FROM locales WHERE id={locale_id}) AS locale_exists"

  private case class FoodAndLocaleValidationRow(food_exists: Option[Boolean], locale_exists: Option[Boolean])

  protected def withLocaleValidation[T](locale: String)(block: => T)(implicit conn: java.sql.Connection): Either[LocaleError, T] = {
    conn.setAutoCommit(false)
    conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ)

    val validation = SQL(localeValidationQuery).on("locale_id" -> locale).executeQuery().as(SqlParser.bool("locale_exists").?.single)

    if (validation.isEmpty) {
      conn.rollback()
      Left(UndefinedLocale)
    } else {
      val result = block
      conn.commit()
      Right(result)
    }
  }

  protected def withFoodAndLocaleValidation[T](foodCode: String, locale: String)(block: => T)(implicit conn: java.sql.Connection): Either[LocalLookupError, T] = {
    conn.setAutoCommit(false)
    conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ)

    val validation = SQL(foodAndLocaleValidationQuery).on("food_code" -> foodCode, "locale_id" -> locale).executeQuery().as(Macro.namedParser[FoodAndLocaleValidationRow].single)

    if (validation.food_exists.isEmpty) {
      conn.commit()
      Left(RecordNotFound)
    } else if (validation.locale_exists.isEmpty) {
      conn.commit()
      Left(UndefinedLocale)
    } else {
      val result = block
      conn.commit()
      Right(result)
    }
  }
}