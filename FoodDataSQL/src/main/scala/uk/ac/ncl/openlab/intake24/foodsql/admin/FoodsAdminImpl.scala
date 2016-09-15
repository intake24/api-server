package uk.ac.ncl.openlab.intake24.foodsql.admin

import java.util.UUID

import scala.Left
import scala.Right

import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

import anorm.Macro
import anorm.NamedParameter
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.foodsql.Util
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.VersionConflict
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.foodsql.shared.FoodPortionSizeShared
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import com.google.inject.Inject
import com.google.inject.Singleton
import javax.sql.DataSource
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordType
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.LocalFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentError
import uk.ac.ncl.openlab.intake24.foodsql.modular.FoodsAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.modular.BrandNamesAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.modular.FoodBrowsingAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.modular.AssociatedFoodsAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.modular.CategoriesAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.modular.BrandNamesUserQueries

@Singleton
class FoodsAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends FoodsAdminImpl

trait FoodsAdminImpl extends FoodsAdminService
    with FoodsAdminQueries
    with BrandNamesAdminQueries
    with FoodBrowsingAdminQueries
    with AssociatedFoodsAdminQueries
    with CategoriesAdminQueries
    with BrandNamesUserQueries {

  private val logger = LoggerFactory.getLogger(classOf[FoodsAdminImpl])

  private case class FoodResultRow(version: UUID, code: String, description: String, local_description: Option[String], do_not_use: Option[Boolean], food_group_id: Long,
    same_as_before_option: Option[Boolean], ready_meal_option: Option[Boolean], reasonable_amount: Option[Int], local_version: Option[UUID])

  private lazy val foodRecordQuery = sqlFromResource("admin/food_record.sql")

  def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord] = tryWithConnection {
    implicit conn =>
      for (
        nutrientTableCodes <- getFoodNutrientTableCodesQuery(code, locale).right;
        portionSizeMethods <- getFoodPortionSizeMethodsQuery(code, locale).right;
        associatedFoods <- getAssociatedFoodsWithHeadersQuery(code, locale).right;
        parentCategories <- getFoodParentCategoriesHeadersQuery(code, locale).right;
        brandNames <- getBrandNamesQuery(code, locale).right;
        localeRestrictions <- getFoodLocaleRestrictionsQuery(code).right;
        record <- {
          val foodQueryResult = SQL(foodRecordQuery).on('food_code -> code, 'locale_id -> locale).executeQuery()

          parseWithLocaleAndFoodValidation(code, foodQueryResult, Macro.namedParser[FoodResultRow].single)().right.map {
            result =>
              FoodRecord(
                MainFoodRecord(result.version, result.code, result.description, result.food_group_id.toInt,
                  InheritableAttributes(result.ready_meal_option, result.same_as_before_option, result.reasonable_amount), parentCategories, localeRestrictions),
                LocalFoodRecord(result.local_version, result.local_description, result.do_not_use.getOrElse(false), nutrientTableCodes, portionSizeMethods,
                  associatedFoods, brandNames))
          }
        }.right
      ) yield record
  }

  def isFoodCode(code: String): Either[DatabaseError, Boolean] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT code FROM foods WHERE code={food_code}""").on('food_code -> code).executeQuery().as(SqlParser.str("code").*).nonEmpty)
  }

  def isFoodCodeAvailable(code: String): Either[DatabaseError, Boolean] = isFoodCode(code).right.map(!_)

  def createFood(newFood: NewMainFoodRecord): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- createFoodsQuery(Seq(newFood)).right;
          _ <- addFoodsToCategoriesQuery(Map(newFood.code -> newFood.parentCategories)).right
        ) yield ()
      }
  }

  def createFoodWithTempCode(newFood: NewMainFoodRecord): Either[DependentCreateError, String] = {
    def tryNextNumber(n: Int): Either[DependentCreateError, String] = {
      if (n > 999)
        Left(DuplicateCode(new RuntimeException("Tried 999 temporary codes, none were available")))
      else {
        val tempCode = "F%03d".format(n)
        createFood(newFood.copy(code = tempCode)) match {
          case Right(()) => Right(tempCode)
          case Left(DuplicateCode(_)) => tryNextNumber(n + 1)
          case Left(x) => Left(x)
        }
      }
    }

    tryNextNumber(0)
  }

  private def mkBatchCategoriesMap(foods: Seq[NewMainFoodRecord]) = {
    val z = Map[String, Seq[String]]()
    foods.foldLeft(z) {
      (map, food) => map + (food.code -> food.parentCategories)
    }
  }

  def createFoods(foods: Seq[NewMainFoodRecord]): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- createFoodsQuery(foods).right;
          _ <- addFoodsToCategoriesQuery(mkBatchCategoriesMap(foods)).right
        ) yield ()
      }
  }

  def deleteAllFoods(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM foods").execute()
      Right(())
  }

  def deleteFoods(foodCodes: Seq[String]): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      if (foodCodes.nonEmpty) {
        withTransaction {

          val params = foodCodes.map {
            code => Seq[NamedParameter]('food_code -> code)
          }

          val rowsAffected = batchSql("DELETE FROM foods WHERE code={food_code}", params).execute()

          if (rowsAffected.forall(_ == 1))
            Right(())
          else
            Left(RecordNotFound)
        }
      } else
        Right(())
  }

  def createLocalFoodRecords(localFoodRecords: Map[String, NewLocalFoodRecord], locale: String): Either[LocalDependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- createLocalFoodsQuery(localFoodRecords, locale).right;
          _ <- createAssociatedFoodsQuery(localFoodRecords.mapValues(_.associatedFoods), locale).right;
          _ <- createBrandNamesQuery(localFoodRecords.mapValues(_.brandNames), locale).right
        ) yield ()
      }
  }

  def updateMainFoodRecord(foodCode: String, foodRecord: MainFoodRecordUpdate): Either[LocalDependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- updateFoodAttributesQuery(foodCode, foodRecord.attributes).right;
          _ <- removeFoodFromAllCategoriesQuery(foodCode).right;
          _ <- addFoodsToCategoriesQuery(Map(foodCode -> foodRecord.parentCategories)).right;
          _ <- updateFoodLocaleRestrictionsQuery(foodCode, foodRecord.localeRestrictions).right;
          _ <- updateFoodQuery(foodCode, foodRecord).right
        ) yield ()
      }
  }

  def updateLocalFoodRecord(foodCode: String, foodLocal: LocalFoodRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- deleteAssociatedFoodsQuery(foodCode, locale).right;
          _ <- createAssociatedFoodsQuery(Map(foodCode -> foodLocal.associatedFoods), locale).right;
          _ <- deleteBrandNamesQuery(foodCode, locale).right;
          _ <- createBrandNamesQuery(Map(foodCode -> foodLocal.brandNames), locale).right;
          _ <- updateLocalFoodQuery(foodCode, foodLocal, locale).right
        ) yield ()
      }
  }
}
