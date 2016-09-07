package uk.ac.ncl.openlab.intake24.foodsql

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import anorm.SQL
import anorm.Macro
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import anorm.SqlParser
import java.sql.Connection
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordType

trait SimpleValidation {

  private val foodAndLocaleValidationQuery = "SELECT (SELECT true FROM foods WHERE code={food_code}) AS food_exists, (SELECT true FROM locales WHERE id={locale_id}) as locale_exists"

  private val categoryAndLocaleValidationQuery = "SELECT (SELECT true FROM categories WHERE code={category_code}) AS category_exists, (SELECT true FROM locales WHERE id={locale_id}) as locale_exists"

  private val localeValidationQuery = "SELECT (SELECT true FROM locales WHERE id={locale_id}) AS locale_exists"

  private case class FoodAndLocaleValidationRow(food_exists: Option[Boolean], locale_exists: Option[Boolean])

  private case class CategoryAndLocaleValidationRow(category_exists: Option[Boolean], locale_exists: Option[Boolean])

  private def checkTransactionIsolation[E >: DatabaseError](conn: Connection)(block: => Either[E, Unit]) = {
    if (conn.getAutoCommit() || (conn.getTransactionIsolation != Connection.TRANSACTION_REPEATABLE_READ))
      Left(DatabaseError("Connection must be in manual commit, repeatable read mode for simple validation", None))
    else
      block
  }

  protected def validateLocale(locale: String)(implicit conn: java.sql.Connection): Either[LocaleError, Unit] = {
    checkTransactionIsolation[LocaleError](conn) {
      val validation = SQL(localeValidationQuery).on("locale_id" -> locale).executeQuery().as(SqlParser.bool("locale_exists").?.single)

      if (validation.isEmpty)
        Left(UndefinedLocale)
      else
        Right(())
    }
  }

  protected def validateFoodAndLocale(foodCode: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Unit] = {
    checkTransactionIsolation[LocalLookupError](conn) {
      val validation = SQL(foodAndLocaleValidationQuery).on("food_code" -> foodCode, "locale_id" -> locale).executeQuery().as(Macro.namedParser[FoodAndLocaleValidationRow].single)

      if (validation.food_exists.isEmpty) {
        Left(RecordNotFound)
      } else if (validation.locale_exists.isEmpty) {
        Left(UndefinedLocale)
      } else {
        Right(())
      }
    }
  }

  protected def validateCategoryAndLocale(categoryCode: String, locale: String)(implicit conn: Connection): Either[LocalLookupError, Unit] = {
    checkTransactionIsolation[LocalLookupError](conn) {
      val validation = SQL(categoryAndLocaleValidationQuery).on("category_code" -> categoryCode, "locale_id" -> locale).executeQuery().as(Macro.namedParser[CategoryAndLocaleValidationRow].single)

      if (validation.category_exists.isEmpty) {
        Left(RecordNotFound)
      } else if (validation.locale_exists.isEmpty) {
        Left(UndefinedLocale)
      } else {
        Right(())
      }
    }
  }

}