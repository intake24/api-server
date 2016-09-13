package uk.ac.ncl.openlab.intake24.foodsql.admin

import java.util.UUID

import scala.Left
import scala.Right

import org.slf4j.LoggerFactory

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.NewLocalCategoryRecord
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import uk.ac.ncl.openlab.intake24.foodsql.modular.CategoriesAdminQueries
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.foodsql.modular.FoodBrowsingAdminQueries
import uk.ac.ncl.openlab.intake24.NewMainCategoryRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesQueries
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCreateError

@Singleton
class CategoriesAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends CategoriesAdminImpl

trait CategoriesAdminImpl extends CategoriesAdminService
    with SimpleValidation
    with CategoriesAdminQueries
    with FoodBrowsingAdminQueries
    with SuperCategoriesQueries {

  private val logger = LoggerFactory.getLogger(classOf[CategoriesAdminImpl])

  def isCategoryCode(code: String): Either[DatabaseError, Boolean] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT code FROM categories WHERE code={category_code}""").on('category_code -> code).executeQuery().as(SqlParser.str("code").singleOpt).nonEmpty)
  }

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean] = isCategoryCode(code).right.map(!_)

  case class CategoryResultRow(version: UUID, local_version: Option[UUID], code: String, description: String, local_description: Option[String], is_hidden: Boolean, same_as_before_option: Option[Boolean],
    ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  private lazy val categoryRecordQuery = sqlFromResource("admin/get_category_record_frv.sql")

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          psm <- categoryPortionSizeMethodsQuery(code, locale).right;
          parentCategories <- getCategoryParentCategoriesHeadersQuery(code, locale).right;
          record <- {
            val categoryQueryResult = SQL(categoryRecordQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

            parseWithLocaleAndCategoryValidation(code, categoryQueryResult, Macro.namedParser[CategoryResultRow].single)().right.map {
              result =>
                CategoryRecord(
                  MainCategoryRecord(result.version, result.code, result.description, result.is_hidden,
                    InheritableAttributes(result.ready_meal_option, result.same_as_before_option, result.reasonable_amount), parentCategories),
                  LocalCategoryRecord(result.local_version, result.local_description, psm))
            }
          }.right
        ) yield record
      }
  }

  def updateMainCategoryRecord(categoryCode: String, record: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- updateCategoryAttributesQuery(categoryCode, record.attributes).right;
          _ <- removeSubcategoryFromAllCategoriesQuery(categoryCode).right;
          _ <- addSubcategoriesToCategoriesQuery(Map(categoryCode -> record.parentCategories)).right;
          _ <- updateCategoryQuery(categoryCode, record).right
        ) yield ()
      }
  }

  def updateLocalCategoryRecord(categoryCode: String, categoryLocal: LocalCategoryRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- updateCategoryPortionSizeMethodsQuery(categoryCode, categoryLocal.portionSize, locale).right;
          _ <- updateCategoryLocalQuery(categoryCode, categoryLocal, locale).right
        ) yield ()
      }
  }
  def addFoodsToCategories(categoryFoods: Map[String, Seq[String]]): Either[ParentError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        addFoodsToCategoriesQuery(categoryFoods)
      }
  }

  def addSubcategoriesToCategories(categorySubcategories: Map[String, Seq[String]]): Either[ParentError, Unit] = tryWithConnection {
    implicit conn =>
      addSubcategoriesToCategoriesQuery(categorySubcategories)
  }

  private val categoriesInsertQuery = "INSERT INTO categories VALUES({code},{description},{is_hidden},{version}::uuid)"

  private val categoriesAttributesInsertQuery = "INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})"

  def createCategory(categoryBase: NewCategory): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      createCategoriesQuery(Seq(categoryBase))
  }

  def createMainCategoryRecord(record: NewMainCategoryRecord): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- createCategoriesQuery(Seq(record.toNewCategory)).right;
          _ <- addSubcategoriesToCategories(Map(record.code -> record.parentCategories)).right
        ) yield ()
      }
  }

  def createMainCategoryRecords(records: Seq[NewMainCategoryRecord]): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- createCategoriesQuery(records.map(_.toNewCategory)).right;
          _ <- addSubcategoriesToCategoriesQuery(records.map(r => (r.code, r.parentCategories)).toMap).right
        ) yield ()
      }
  }

  def createCategories(categories: Seq[NewCategory]): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      createCategoriesQuery(categories)
  }

  def createLocalCategoryRecord(foodCode: String, record: NewLocalCategoryRecord, locale: String): Either[LocalCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        createLocalCategoriesQuery(Map(foodCode -> record), locale)
      }
  }

  def createLocalCategoryRecords(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[LocalCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        createLocalCategoriesQuery(localCategoryRecords, locale)
      }
  }

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM categories WHERE code={category_code}").on('category_code -> categoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(RecordNotFound)
  }

  def deleteAllCategories(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM categories").execute()
      Right(())
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM foods_categories WHERE food_code={food_code} AND category_code={category_code}").on('category_code -> categoryCode, 'food_code -> foodCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        // TODO: Could be food or category, needs better validation
        Left(RecordNotFound)
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>

      val rowsAffected = SQL("DELETE FROM categories_categories WHERE subcategory_code={subcategory_code} AND category_code={category_code}")
        .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        // TODO: Could be food or category, needs better validation
        Left(RecordNotFound)
  }
}
