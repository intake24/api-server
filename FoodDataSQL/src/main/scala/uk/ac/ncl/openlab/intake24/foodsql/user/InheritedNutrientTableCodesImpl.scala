package uk.ac.ncl.openlab.intake24.foodsql.user

import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause

import anorm.SQL
import anorm.Macro

trait InheritedNutrientTableCodesImpl extends SqlResourceLoader with FirstRowValidation {
  private case class NutrientTableRow(nutrient_table_id: String, nutrient_table_record_id: String)

  private lazy val inheritedTableCodesQuery = sqlFromResource("user/inherited_nutrient_table_codes.sql")

  private def localNutrientTableCodes(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Map[String, String]] = {
    val result = SQL(inheritedTableCodesQuery).on('food_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(code, result, Macro.namedParser[NutrientTableRow].+)(Seq(FirstRowValidationClause("nutrient_table_id", Right(List())))).right.map {
      _.map {
        case NutrientTableRow(id, code) => (id -> code)
      }.toMap
    }
  }

  protected case class ResolvedNutrientTableCodes(codes: Map[String, String], sourceLocale: SourceLocale)

  protected def resolveNutrientTableCodes(foodCode: String, locale: String, prototypeLocale: Option[String])(implicit conn: java.sql.Connection): Either[LocalLookupError, ResolvedNutrientTableCodes] = {
    localNutrientTableCodes(foodCode, locale).right.flatMap {
      localCodes =>
        (localCodes.isEmpty, prototypeLocale) match {
          case (true, Some(pl)) => {
            localNutrientTableCodes(foodCode, pl).right.map {
              codes => ResolvedNutrientTableCodes(codes, SourceLocale.Prototype(pl))
            }
          }
          case _ => Right(ResolvedNutrientTableCodes(localCodes, SourceLocale.Current(locale)))
        }
    }
  }
}