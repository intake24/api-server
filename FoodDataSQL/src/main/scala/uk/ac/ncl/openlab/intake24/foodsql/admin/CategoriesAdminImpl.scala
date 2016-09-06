package uk.ac.ncl.openlab.intake24.foodsql.admin

import java.util.UUID

import uk.ac.ncl.openlab.intake24.CategoryRecord
import anorm._
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import org.postgresql.util.PSQLException

import anorm.NamedParameter.symbol
import scala.Left
import scala.Right

import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.foodsql.Util

import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.VersionConflict
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import org.slf4j.LoggerFactory
import java.sql.BatchUpdateException
import scala.collection.mutable.ArrayBuffer
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.foodsql.shared.FoodPortionSizeShared
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import com.google.inject.Inject
import javax.sql.DataSource
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordType

class CategoriesAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends CategoriesAdminImpl

trait CategoriesAdminImpl extends CategoriesAdminService
    with SqlDataService
    with SqlResourceLoader
    with FirstRowValidation
    with FoodPortionSizeShared
    with AdminErrorMessagesShared
    with SimpleValidation {

  private val logger = LoggerFactory.getLogger(classOf[CategoriesAdminImpl])

  private def categoryCodeFkConstraintFailedMessage(categoryCode: String) =
    s"Category code $categoryCode is not defined. Either an invalid code was supplied or the category was deleted or had its code changed by someone else."

  private val categoryCodeFkConstraintFailedCode = "invalid_category_code"

  private def categoryCodePkConstraintFailedMessage(categoryCode: String) =
    s"Category code $categoryCode already exists. Duplicate category codes are not allowed."

  private val categoryCodePkConstraintFailedCode = "duplicate_category_code"

  private def foodNotInCategoryMessage(categoryCode: String, foodCode: String) =
    s"Food $foodCode is not in category $categoryCode, so it cannot be deleted."

  private val foodNotInCategoryCode = "food_not_in_category"

  private def subcategoryNotInCategoryMessage(categoryCode: String, subcategoryCode: String) =
    s"Subcategory $subcategoryCode is not in category $categoryCode, so it cannot be deleted."

  private val subcategoryNotInCategoryCode = "subcategory_not_in_category"

  private def cannotAddCategoryToItselfMessage(categoryCode: String) =
    s"Cannot add $categoryCode to itself."

  private val cannotAddCategoryToItselfCode = "cannot_add_category_to_itself"

  def isCategoryCode(code: String): Either[DatabaseError, Boolean] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT code FROM categories WHERE code={category_code}""").on('category_code -> code).executeQuery().as(SqlParser.str("code").singleOpt).nonEmpty)
  }

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean] = isCategoryCode(code).right.map(!_)

  private lazy val categoryPsmQuery = sqlFromResource("shared/category_portion_size_methods.sql")

  def categoryPortionSizeMethods(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[PortionSizeMethod]] = {
    val psmResults = SQL(categoryPsmQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, psmResults, psmResultRowParser.+)(Seq(FirstRowValidationClause("id", Right(List())))).right.map(mkPortionSizeMethods)
  }

  case class CategoryResultRow(version: UUID, local_version: Option[UUID], code: String, description: String, local_description: Option[String], is_hidden: Boolean, same_as_before_option: Option[Boolean],
    ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  private lazy val categoryRecordQuery = sqlFromResource("admin/get_category_record_frv.sql")

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] = tryWithConnection {
    implicit conn =>
      categoryPortionSizeMethods(code, locale).right.flatMap {
        psm =>
          val categoryQueryResult = SQL(categoryRecordQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

          parseWithLocaleAndCategoryValidation(code, categoryQueryResult, Macro.namedParser[CategoryResultRow].single)().right.map {
            result =>
              CategoryRecord(
                MainCategoryRecord(result.version, result.code, result.description, result.is_hidden,
                  InheritableAttributes(result.ready_meal_option, result.same_as_before_option, result.reasonable_amount)),
                LocalCategoryRecord(result.local_version, result.local_description, psm))
          }
      }
  }

  def updateMainCategoryRecord(categoryCode: String, categoryBase: MainCategoryRecord): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      try {
        SQL("DELETE FROM categories_attributes WHERE category_code={category_code}")
          .on('category_code -> categoryCode).execute()

        SQL("INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})")
          .on('category_code -> categoryCode, 'same_as_before_option -> categoryBase.attributes.sameAsBeforeOption,
            'ready_meal_option -> categoryBase.attributes.readyMealOption, 'reasonable_amount -> categoryBase.attributes.reasonableAmount).execute()

        val rowsAffected = SQL("UPDATE categories SET code = {new_code}, description={description}, is_hidden={is_hidden}, version={new_version}::uuid WHERE code={category_code} AND version={base_version}::uuid")
          .on('category_code -> categoryCode, 'base_version -> categoryBase.version,
            'new_version -> UUID.randomUUID(), 'new_code -> categoryBase.code, 'description -> categoryBase.englishDescription, 'is_hidden -> categoryBase.isHidden)
          .executeUpdate()

        if (rowsAffected == 1) {
          conn.commit()
          Right(())

        } else {
          conn.rollback()
          Left(VersionConflict)
        }

      } catch {
        case e: PSQLException => {
          e.getServerErrorMessage.getConstraint match {
            case "categories_attributes_category_code_fk" => Left(RecordNotFound(RecordType.Category, categoryCode))
            case _ => throw e
          }
        }
      }
  }

  private val categoriesInsertPsmParamsQuery = "INSERT INTO categories_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})"

  private val categoriesInsertLocalQuery = "INSERT INTO categories_local VALUES({category_code}, {locale_id}, {local_description}, {version}::uuid)"

  private val categoriesPsmInsertQuery = "INSERT INTO categories_portion_size_methods VALUES(DEFAULT, {category_code}, {locale_id}, {method}, {description}, {image_url}, {use_for_recipes})"

  private def updatePortionSizeMethods(categoryCode: String, locale: String, portionSize: Seq[PortionSizeMethod])(implicit conn: java.sql.Connection): Either[LocalUpdateError, Unit] = {
    val errors = Map("categories_portion_size_methods_categories_code_fk" -> RecordNotFound(RecordType.Category, categoryCode),
      "categories_portion_size_methods_locale_id_fk" -> UndefinedLocale)

    SQL("DELETE FROM categories_portion_size_methods WHERE category_code={category_code} AND locale_id={locale_id}")
      .on('category_code -> categoryCode, 'locale_id -> locale).execute()

    if (portionSize.nonEmpty) {
      tryWithConstraintsCheck(errors) {
        val psmParams = portionSize.map(ps => Seq[NamedParameter]('category_code -> categoryCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))

        val psmKeys = Util.batchKeys(batchSql(categoriesPsmInsertQuery, psmParams))

        val psmParamParams = portionSize.zip(psmKeys).flatMap {
          case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
        }

        if (psmParamParams.nonEmpty)
          batchSql(categoriesInsertPsmParamsQuery, psmParamParams).execute()

        Right(())
      }
    } else Right(())
  }

  private def updateCategoryLocalRecordImpl(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord)(implicit conn: java.sql.Connection): Either[LocalUpdateError, Unit] = {
    val rowsAffected = SQL("UPDATE categories_local SET version = {new_version}::uuid, local_description = {local_description} WHERE category_code = {category_code} AND locale_id = {locale_id} AND version = {base_version}::uuid")
      .on('category_code -> categoryCode, 'locale_id -> locale, 'base_version -> categoryLocal.version, 'new_version -> UUID.randomUUID(), 'local_description -> categoryLocal.localDescription)
      .executeUpdate()

    if (rowsAffected == 1) {
      Right(())
    } else {
      Left(VersionConflict)
    }
  }

  private def insertCategoryLocalRecordImpl(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord)(implicit conn: java.sql.Connection): Either[LocalUpdateError, Unit] = {
    tryWithConstraintCheck("categories_local_pk", VersionConflict) {
      SQL(categoriesInsertLocalQuery)
        .on('category_code -> categoryCode, 'locale_id -> locale, 'local_description -> categoryLocal.localDescription, 'version -> UUID.randomUUID())
        .execute()
      Right(())
    }
  }

  def updateLocalCategoryRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)
      conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ)

      val result = validateCategoryAndLocale(categoryCode, locale).right.flatMap {
        _ =>
          updatePortionSizeMethods(categoryCode, locale, categoryLocal.portionSize).right.flatMap {
            _ =>
              categoryLocal.version match {
                case Some(version) => updateCategoryLocalRecordImpl(categoryCode, locale, categoryLocal)
                case None => insertCategoryLocalRecordImpl(categoryCode, locale, categoryLocal)
              }
          }
      }

      result match {
        case x @ Left(error) => {
          conn.rollback()
          x
        }
        case x @ Right(()) => {
          conn.commit()
          x
        }
      }
  }

  private val foodsCategoriesInsertQuery = "INSERT INTO foods_categories VALUES(DEFAULT, {food_code},{category_code})"

  def addFoodToCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      try {
        SQL(foodsCategoriesInsertQuery)
          .on('category_code -> categoryCode, 'food_code -> foodCode).execute()
        Right(())
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "foods_categories_unique" => Right(())
            case "foods_categories_category_code_fk" => Left(RecordNotFound(RecordType.Category, categoryCode))
            case "foods_categories_food_code_fk" => Left(RecordNotFound(RecordType.Food, foodCode))
            case _ => throw e
          }
      }
  }

  def removeFoodFromAllCategoriesComposable(foodCode: String)(implicit conn: java.sql.Connection): Either[UpdateError, Unit] = {
    SQL("DELETE FROM foods_categories WHERE food_code={food_code}").on('food_code -> foodCode).execute()
    Right(())
  }

  def addFoodsToCategoriesComposable(categoryFoods: Map[String, Seq[String]])(implicit conn: java.sql.Connection): Either[UpdateError, Unit] = {
    try {
      val foodCategoryParams =
        categoryFoods.flatMap {
          case (code, foods) =>
            foods.map(f => Seq[NamedParameter]('food_code -> f, 'category_code -> code))
        }.toSeq

      if (!foodCategoryParams.isEmpty) {

        logger.debug("Writing " + foodCategoryParams.size + " food parent category records")
        batchSql(foodsCategoriesInsertQuery, foodCategoryParams).execute()

        Right(())
      } else {
        logger.debug("No foods contained in any of the categories")
        Right(())
      }
    } catch {
      case e: BatchUpdateException => e.getNextException match {
        case e2: PSQLException => e2.getServerErrorMessage.getConstraint match {
          case "foods_categories_unique" => Right(())
          case "foods_categories_category_code_fk" => Left(RecordNotFound(RecordType.Category, "Not available for batch operations"))
          case "foods_categories_food_code_fk" => Left(RecordNotFound(RecordType.Food, "Not available for batch operations"))
          case _ => throw e
        }
        case _ => throw e
      }
    }
  }

  def addFoodsToCategories(categoryFoods: Map[String, Seq[String]]): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        addFoodsToCategoriesComposable(categoryFoods)
      }
  }

  private val categoriesCategoriesInsertQuery = "INSERT INTO categories_categories VALUES(DEFAULT, {subcategory_code},{category_code})"

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      if (categoryCode == subcategoryCode) {
        Left(DatabaseError(cannotAddCategoryToItselfMessage(categoryCode), None))
      } else
        try {
          SQL(categoriesCategoriesInsertQuery)
            .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).execute()
          Right(())
        } catch {
          case e: PSQLException =>
            e.getServerErrorMessage.getConstraint match {
              case "categories_categories_unique" => Right(())
              case "categories_categories_subcategory_code_fk" => Left(RecordNotFound(RecordType.Category, subcategoryCode))
              case "categories_categories_category_code_fk" => Left(RecordNotFound(RecordType.Category, categoryCode))
              case _ => throw e
            }
        }
  }

  def addSubcategoriesToCategories(categorySubcategories: Map[String, Seq[String]]): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      try {
        categorySubcategories.find {
          case (code, subcats) => subcats.contains(code)
        } match {
          case Some((code, _)) => Left(DatabaseError(cannotAddCategoryToItselfMessage(code), None))
          case None => {

            val categoryCategoryParams =
              categorySubcategories.flatMap {
                case (code, subcats) =>
                  subcats.map(c => Seq[NamedParameter]('subcategory_code -> c, 'category_code -> code))
              }.toSeq

            if (!categoryCategoryParams.isEmpty) {
              conn.setAutoCommit(false)
              logger.debug("Writing " + categoryCategoryParams.size + " category parent category records")
              batchSql(categoriesCategoriesInsertQuery, categoryCategoryParams).execute()
              conn.commit()
              Right(())
            } else
              logger.debug("No subcategories contained in any of the categories")
            Right(())
          }
        }
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "categories_categories_unique" => Right(())
            case "categories_categories_subcategory_code_fk" => Left(RecordNotFound(RecordType.Category, "Not available for batch operations"))
            case "categories_categories_category_code_fk" => Left(RecordNotFound(RecordType.Category, "Not available for batch operations"))
            case _ => throw e
          }
      }
  }

  private val categoriesInsertQuery = "INSERT INTO categories VALUES({code},{description},{is_hidden},{version}::uuid)"

  private val categoriesAttributesInsertQuery = "INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})"

  def createCategory(categoryBase: NewCategory): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      try {
        SQL(categoriesInsertQuery)
          .on('code -> categoryBase.code, 'description -> categoryBase.englishDescription, 'is_hidden -> categoryBase.isHidden, 'version -> UUID.randomUUID())
          .execute()

        SQL(categoriesAttributesInsertQuery)
          .on('category_code -> categoryBase.code, 'same_as_before_option -> categoryBase.attributes.sameAsBeforeOption,
            'ready_meal_option -> categoryBase.attributes.readyMealOption, 'reasonable_amount -> categoryBase.attributes.reasonableAmount).execute()

        Right(())
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "categories_pk" => Left(DuplicateCode)
            case _ => throw e
          }
      }
  }

  def createCategories(categories: Seq[NewCategory]): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      if (!categories.isEmpty) {
        conn.setAutoCommit(false)

        logger.debug("Writing " + categories.size + " categories to database")

        val categoryParams =
          categories.map(c => Seq[NamedParameter]('code -> c.code, 'description -> c.englishDescription, 'is_hidden -> c.isHidden, 'version -> UUID.randomUUID()))

        tryWithConstraintCheck("categories_pk", DuplicateCode) {
          batchSql(categoriesInsertQuery, categoryParams).execute()

          val categoryAttributeParams =
            categories.map(c => Seq[NamedParameter]('category_code -> c.code, 'same_as_before_option -> c.attributes.sameAsBeforeOption,
              'ready_meal_option -> c.attributes.readyMealOption, 'reasonable_amount -> c.attributes.reasonableAmount))

          batchSql(categoriesAttributesInsertQuery, categoryAttributeParams).execute()

          conn.commit()
          Right(())
        }
      } else {
        logger.debug("Create categories request with empty foods list")
        Right(())
      }
  }

  def createLocalCategories(localCategoryRecords: Map[String, LocalCategoryRecord], locale: String): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>

      if (localCategoryRecords.nonEmpty) {

        val localCategoryRecordsSeq = localCategoryRecords.toSeq

        logger.debug(s"Writing ${localCategoryRecordsSeq.size} new local category records to database")

        conn.setAutoCommit(false)

        tryWithConstraintCheck("categories_local_pk", DuplicateCode) {

          val localCategoryParams =
            localCategoryRecordsSeq.map {
              case (code, local) =>
                Seq[NamedParameter]('category_code -> code, 'locale_id -> locale, 'local_description -> local.localDescription, 'version -> local.version.getOrElse(UUID.randomUUID()))
            }

          batchSql(categoriesInsertLocalQuery, localCategoryParams).execute()

          val psmParams =
            localCategoryRecordsSeq.flatMap {
              case (code, local) =>
                local.portionSize.map {
                  ps =>
                    Seq[NamedParameter]('category_code -> code, 'locale_id -> locale, 'method -> ps.method,
                      'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes)
                }
            }

          if (!psmParams.isEmpty) {
            logger.debug("Writing " + psmParams.size + " category portion size method definitions")

            val keys = Util.batchKeys(batchSql(categoriesPsmInsertQuery, psmParams))

            val psmParamParams = localCategoryRecordsSeq.flatMap(_._2.portionSize).zip(keys).flatMap {
              case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
            }

            if (!psmParamParams.isEmpty) {
              logger.debug("Writing " + psmParamParams.size + " category portion size method parameters")
              batchSql(categoriesInsertPsmParamsQuery, psmParamParams).execute()
            } else
              logger.debug("No category portion size method parameters found")
          } else
            logger.debug("No category portion size method records found")

          conn.commit()
          Right(())
        }
      } else {
        logger.debug("Categories file contains no records")
        Right(())
      }
  }

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM categories WHERE code={category_code}").on('category_code -> categoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(RecordNotFound(RecordType.Category, categoryCode))
  }

  def deleteAllCategories(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM categories").execute()
      Right(())
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM foods_categories WHERE food_code={food_code} AND category_code={category_code}").on('category_code -> categoryCode, 'food_code -> foodCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        // TODO: Could be food or category, needs better validation
        Left(RecordNotFound(RecordType.Food, foodCode))
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>

      val rowsAffected = SQL("DELETE FROM categories_categories WHERE subcategory_code={subcategory_code} AND category_code={category_code}")
        .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        // TODO: Could be food or category, needs better validation
        Left(RecordNotFound(RecordType.Category, subcategoryCode))
  }
}
