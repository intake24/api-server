package uk.ac.ncl.openlab.intake24.foodsql

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCategoryCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import anorm.SqlQueryResult
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalFoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedCode
import anorm.ResultSetParser
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CategoryCodeError

case class FirstRowValidationClause[E, T](columnName: String, resultIfNull: Either[E, T])

trait FirstRowValidation {
    def parseWithFirstRowValidation[E >: DatabaseError, T](result: SqlQueryResult, validation: Seq[FirstRowValidationClause[E, T]], parser: ResultSetParser[T])(implicit connection: java.sql.Connection): Either[E, T] = {
    result.withResult {
      cursorOpt =>
        val firstRow = cursorOpt.get.row
        val columns = firstRow.asMap

        def isNull(columnName: String) = columns(columnName) match {
          case None => true
          case _ => false
        }

        validation.find {
          case FirstRowValidationClause(name, _) => isNull(name)
        } match {
          case Some(FirstRowValidationClause(_, result)) => result
          case None => parser(cursorOpt) match {
            case anorm.Success(parsed) => Right(parsed)
            case anorm.Error(e) => Left(DatabaseError(e.message, None))
          }
        }
    } match {
      case Left(errors) => Left(DatabaseError(errors.head.getMessage, Some(errors.head)))
      case Right(data) => data
    }
  }

  def localeValidation[T]: Seq[FirstRowValidationClause[LocaleError, T]] =
    Seq(FirstRowValidationClause("locale_id", Left(UndefinedLocale)))
    
  def foodValidation[T]: Seq[FirstRowValidationClause[FoodCodeError, T]] =
    Seq(FirstRowValidationClause("food_code", Left(UndefinedCode)))
    
  def categoryValidation[T]: Seq[FirstRowValidationClause[CategoryCodeError, T]] =
    Seq(FirstRowValidationClause("category_code", Left(UndefinedCode)))    

  def localeAndFoodCodeValidation[T]: Seq[FirstRowValidationClause[LocalFoodCodeError, T]] =
    Seq(FirstRowValidationClause("food_code", Left(UndefinedCode)), FirstRowValidationClause("locale_id", Left(UndefinedLocale)))

  def localeAndCategoryCodeValidation[T]: Seq[FirstRowValidationClause[LocalCategoryCodeError, T]] =
    Seq(FirstRowValidationClause("category_code", Left(UndefinedCode)), FirstRowValidationClause("locale_id", Left(UndefinedLocale)))

  def parseWithLocaleValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LocaleError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LocaleError, T] =
    parseWithFirstRowValidation(result, localeValidation[T] ++ additionalValidation, parser)
    
  def parseWithFoodValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[FoodCodeError, T]] = Seq())(implicit conn: java.sql.Connection): Either[FoodCodeError, T] =
    parseWithFirstRowValidation(result, foodValidation[T] ++ additionalValidation, parser)
    
  def parseWithCategoryValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[CategoryCodeError, T]] = Seq())(implicit conn: java.sql.Connection): Either[CategoryCodeError, T] =
    parseWithFirstRowValidation(result, categoryValidation[T] ++ additionalValidation, parser)        
    
  def parseWithLocaleAndFoodValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LocalFoodCodeError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LocalFoodCodeError, T] =
    parseWithFirstRowValidation(result, localeAndFoodCodeValidation[T] ++ additionalValidation, parser)

  def parseWithLocaleAndCategoryValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LocalCategoryCodeError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LocalCategoryCodeError, T] =
    parseWithFirstRowValidation(result, localeAndCategoryCodeValidation[T] ++ additionalValidation, parser)
}