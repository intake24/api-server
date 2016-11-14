package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader

import anorm._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.QuickSearchService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError

import com.google.inject.Inject
import com.google.inject.Singleton
import javax.sql.DataSource
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService

@Singleton
class QuickSearchAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends QuickSearchAdminImpl

trait QuickSearchAdminImpl extends QuickSearchService with FoodDataSqlService with HeaderRows with SqlResourceLoader {
  
  private lazy val foodsQuickSearchQuery = sqlFromResource("admin/foods_quick_search.sql")
  
  def searchFoods(searchTerm: String, locale: String): Either[UnexpectedDatabaseError, Seq[FoodHeader]] = tryWithConnection {
    implicit conn =>
      val lowerCaseTerm = searchTerm.toLowerCase

      Right(SQL(foodsQuickSearchQuery).on('pattern -> s"%${lowerCaseTerm}%", 'locale_id -> locale).executeQuery().as(Macro.namedParser[FoodHeaderRow].*).map(_.asFoodHeader))
  }
  
  private lazy val categoriesQuickSearchQuery = sqlFromResource("admin/categories_quick_search.sql")

  def searchCategories(searchTerm: String, locale: String): Either[UnexpectedDatabaseError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      val lowerCaseTerm = searchTerm.toLowerCase

      Right(SQL(categoriesQuickSearchQuery).on('pattern -> s"%${lowerCaseTerm}%", 'locale_id -> locale).executeQuery().as(Macro.namedParser[CategoryHeaderRow].*).map(_.asCategoryHeader))

  }
}
