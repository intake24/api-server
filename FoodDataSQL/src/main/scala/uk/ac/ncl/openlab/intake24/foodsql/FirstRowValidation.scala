package uk.ac.ncl.openlab.intake24.foodsql

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import anorm.SqlQueryResult
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import anorm.ResultSetParser
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import anorm.AnormUtil.isNull

case class FirstRowValidationClause[E, T](columnName: String, resultIfNull: Either[E, T])

trait FirstRowValidation {
  
    // see http://stackoverflow.com/a/38793141/622196 for explanation
  
    def parseWithFirstRowValidation[E >: DatabaseError, T](result: SqlQueryResult, validation: Seq[FirstRowValidationClause[E, T]], parser: ResultSetParser[T])(implicit connection: java.sql.Connection): Either[E, T] = {
    result.withResult {
      cursorOpt =>
        val firstRow = cursorOpt.get.row

        validation.find {
          case FirstRowValidationClause(name, _) => isNull(firstRow, name)
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
    
  def foodValidation[T]: Seq[FirstRowValidationClause[LookupError, T]] =
    Seq(FirstRowValidationClause("food_code", Left(RecordNotFound)))
    
  def categoryValidation[T]: Seq[FirstRowValidationClause[LookupError, T]] =
    Seq(FirstRowValidationClause("category_code", Left(RecordNotFound)))    

  def localeAndFoodCodeValidation[T]: Seq[FirstRowValidationClause[LocalLookupError, T]] =
    Seq(FirstRowValidationClause("food_code", Left(RecordNotFound)), FirstRowValidationClause("locale_id", Left(UndefinedLocale)))

  def localeAndCategoryCodeValidation[T]: Seq[FirstRowValidationClause[LocalLookupError, T]] =
    Seq(FirstRowValidationClause("category_code", Left(RecordNotFound)), FirstRowValidationClause("locale_id", Left(UndefinedLocale)))

  def parseWithLocaleValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LocaleError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LocaleError, T] =
    parseWithFirstRowValidation(result, localeValidation[T] ++ additionalValidation, parser)
    
  def parseWithFoodValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LookupError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LookupError, T] =
    parseWithFirstRowValidation(result, foodValidation[T] ++ additionalValidation, parser)
    
  def parseWithCategoryValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LookupError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LookupError, T] =
    parseWithFirstRowValidation(result, categoryValidation[T] ++ additionalValidation, parser)        
    
  def parseWithLocaleAndFoodValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LocalLookupError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LocalLookupError, T] =
    parseWithFirstRowValidation(result, localeAndFoodCodeValidation[T] ++ additionalValidation, parser)

  def parseWithLocaleAndCategoryValidation[T](result: SqlQueryResult, parser: ResultSetParser[T])(additionalValidation: Seq[FirstRowValidationClause[LocalLookupError, T]] = Seq())(implicit conn: java.sql.Connection): Either[LocalLookupError, T] =
    parseWithFirstRowValidation(result, localeAndCategoryCodeValidation[T] ++ additionalValidation, parser)
}