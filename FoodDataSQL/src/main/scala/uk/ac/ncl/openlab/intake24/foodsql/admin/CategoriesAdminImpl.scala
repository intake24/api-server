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
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CategoryCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCategoryCodeError
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.VersionConflict
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedCode
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

trait CategoriesAdminImpl extends CategoriesAdminService with SqlDataService with SqlResourceLoader with FirstRowValidation with FoodPortionSizeShared with AdminErrorMessagesShared {

  val logger = LoggerFactory.getLogger(classOf[CategoriesAdminImpl])

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

  def categoryPortionSizeMethods(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalCategoryCodeError, Seq[PortionSizeMethod]] = {
    val psmResults = SQL(categoryPsmQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(psmResults, psmResultRowParser.+)(Seq(FirstRowValidationClause("id", Right(List())))).right.map(mkPortionSizeMethods)
  }

  case class CategoryResultRow(version: UUID, local_version: Option[UUID], code: String, description: String, local_description: Option[String], is_hidden: Boolean, same_as_before_option: Option[Boolean],
    ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  private lazy val categoryRecordQuery = sqlFromResource("admin/category_record.sql")

  def categoryRecord(code: String, locale: String): Either[LocalCategoryCodeError, CategoryRecord] = tryWithConnection {
    implicit conn =>
      categoryPortionSizeMethods(code, locale).right.flatMap {
        psm =>
          val categoryQueryResult = SQL(categoryRecordQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

          parseWithLocaleAndCategoryValidation(categoryQueryResult, Macro.namedParser[CategoryResultRow].single)().right.map {
            result =>
              CategoryRecord(
                MainCategoryRecord(result.version, result.code, result.description, result.is_hidden,
                  InheritableAttributes(result.ready_meal_option, result.same_as_before_option, result.reasonable_amount)),
                LocalCategoryRecord(result.local_version, result.local_description, psm))
          }
      }
  }

  def updateCategoryBase(categoryCode: String, categoryBase: MainCategoryRecord): Either[UpdateError, Unit] = tryWithConnection {
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
            case "categories_attributes_category_code_fk" => Left(UndefinedCode)
            case _ => throw e
          }
        }
      }
  }

  val categoriesInsertPsmParamsQuery = "INSERT INTO categories_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})"

  val categoriesInsertLocalQuery = "INSERT INTO categories_local VALUES({category_code}, {locale_id}, {local_description}, {version}::uuid)"

  val categoriesPsmInsertQuery = "INSERT INTO categories_portion_size_methods VALUES(DEFAULT, {category_code}, {locale_id}, {method}, {description}, {image_url}, {use_for_recipes})"

  def updateCategoryLocal(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      try {
        SQL("DELETE FROM categories_portion_size_methods WHERE category_code={category_code} AND locale_id={locale_id}")
          .on('category_code -> categoryCode, 'locale_id -> locale).execute()

        if (categoryLocal.portionSize.nonEmpty) {
          val psmParams = categoryLocal.portionSize.flatMap(ps => Seq[NamedParameter]('category_code -> categoryCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))

          val psmKeys = Util.batchKeys(BatchSql(categoriesPsmInsertQuery, psmParams))

          val psmParamParams = categoryLocal.portionSize.zip(psmKeys).flatMap {
            case (psm, id) => psm.parameters.flatMap(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
          }

          if (psmParamParams.nonEmpty)
            BatchSql(categoriesInsertPsmParamsQuery, psmParamParams).execute()
        }

        categoryLocal.version match {
          case Some(version) => {

            // categories_local_category_code_fk
            val rowsAffected = SQL("UPDATE categories_local SET version = {new_version}::uuid, local_description = {local_description} WHERE category_code = {category_code} AND locale_id = {locale_id} AND version = {base_version}::uuid")
              .on('category_code -> categoryCode, 'locale_id -> locale, 'base_version -> categoryLocal.version, 'new_version -> UUID.randomUUID(), 'local_description -> categoryLocal.localDescription)
              .executeUpdate()

            if (rowsAffected == 1) {
              conn.commit()
              Right(())
            } else {
              conn.rollback()
              Left(VersionConflict)
            }
          }
          case None => {
            try {
              SQL(categoriesInsertLocalQuery)
                .on('category_code -> categoryCode, 'locale_id -> locale, 'local_description -> categoryLocal.localDescription, 'version -> UUID.randomUUID())
                .execute()
              conn.commit()
              Right(())
            } catch {
              case e: PSQLException =>
                if (e.getServerErrorMessage.getConstraint == "categories_local_pk") {
                  conn.rollback()
                  Left(VersionConflict)
                } else
                  throw e
            }
          }
        }
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "categories_portion_size_methods_categories_code_fk" | "categories_local_category_code_fk" => Left(UndefinedCode)
            case "categories_portion_size_methods_locale_id_fk" | "categories_local_locale_id_fk" => Left(UndefinedLocale)
            case _ => throw e
          }
      }
  }

  val foodsCategoriesInsertQuery = "INSERT INTO foods_categories VALUES(DEFAULT, {food_code},{category_code})"

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
            case "foods_categories_category_code_fk" => Left(UndefinedCode)
            case "foods_categories_food_code_fk" => Left(UndefinedCode)
            case _ => throw e
          }
      }
  }

  def addFoodsToCategories(categoryFoods: Map[String, Seq[String]]): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      try {
        val foodCategoryParams =
          categoryFoods.flatMap {
            case (code, foods) =>
              foods.map(f => Seq[NamedParameter]('food_code -> f, 'category_code -> code))
          }.toSeq

        if (!foodCategoryParams.isEmpty) {
          conn.setAutoCommit(false)
          logger.info("Writing " + foodCategoryParams.size + " food parent category records")
          BatchSql(foodsCategoriesInsertQuery, foodCategoryParams).execute()
          conn.commit()
          Right(())
        } else {
          logger.warn("No foods contained in any of the categories")
          Right(())
        }
      } catch {
        case e: BatchUpdateException => e.getNextException match {
          case e2: PSQLException => e2.getServerErrorMessage.getConstraint match {
            case "foods_categories_unique" => Right(())
            case "foods_categories_category_code_fk" => Left(UndefinedCode)
            case "foods_categories_food_code_fk" => Left(UndefinedCode)
            case _ => throw e
          }
          case _ => throw e
        }
      }
  }

  val categoriesCategoriesInsertQuery = "INSERT INTO categories_categories VALUES(DEFAULT, {subcategory_code},{category_code})"

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
              case "categories_categories_subcategory_code_fk" => Left(UndefinedCode)
              case "categories_categories_category_code_fk" => Left(UndefinedCode)
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
              logger.info("Writing " + categoryCategoryParams.size + " category parent category records")
              BatchSql(categoriesCategoriesInsertQuery, categoryCategoryParams).execute()
              conn.commit()
              Right(())
            } else
              logger.warn("No subcategories contained in any of the categories")
            Right(())
          }
        }
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "categories_categories_unique" => Right(())
            case "categories_categories_subcategory_code_fk" => Left(UndefinedCode)
            case "categories_categories_category_code_fk" => Left(UndefinedCode)
            case _ => throw e
          }
      }
  }

  val categoriesInsertQuery = "INSERT INTO categories VALUES({code},{description},{is_hidden},{version}::uuid)"

  val categoriesAttributesInsertQuery = "INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})"

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

        logger.info("Writing " + categories.size + " categories to database")

        val categoryParams =
          categories.map(c => Seq[NamedParameter]('code -> c.code, 'description -> c.englishDescription, 'is_hidden -> c.isHidden, 'version -> UUID.randomUUID()))

        BatchSql(categoriesInsertQuery, categoryParams).execute()

        val categoryAttributeParams =
          categories.map(c => Seq[NamedParameter]('category_code -> c.code, 'same_as_before_option -> c.attributes.sameAsBeforeOption,
            'ready_meal_option -> c.attributes.readyMealOption, 'reasonable_amount -> c.attributes.reasonableAmount))

        BatchSql(categoriesAttributesInsertQuery, categoryAttributeParams).execute()

        conn.commit()

        Right(())
      } else {
        logger.warn("Create categories request with empty foods list")
        Right(())
      }
  }

  def createLocalCategories(localCategoryRecords: Map[String, LocalCategoryRecord], locale: String): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>

      if (localCategoryRecords.nonEmpty) {

        val localCategoryRecordsSeq = localCategoryRecords.toSeq

        logger.info(s"Writing ${localCategoryRecordsSeq.size} new local category records to database")

        conn.setAutoCommit(false)

        val localCategoryParams =
          localCategoryRecordsSeq.flatMap {
            case (code, local) =>
              Seq[NamedParameter]('category_code -> code, 'locale_id -> locale, 'local_description -> local.localDescription, 'version -> local.version.getOrElse(UUID.randomUUID()))
          }

        BatchSql(categoriesInsertLocalQuery, localCategoryParams).execute()

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
          logger.info("Writing " + psmParams.size + " category portion size method definitions")

          val statement = BatchSql(categoriesPsmInsertQuery, psmParams).getFilledStatement(conn, true)

          statement.executeBatch()

          val rs = statement.getGeneratedKeys()
          val buf = ArrayBuffer[Long]()

          while (rs.next()) {
            buf += rs.getLong(1)
          }

          val psmParamParams = localCategoryRecordsSeq.flatMap(_._2.portionSize).zip(buf).flatMap {
            case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
          }

          if (!psmParamParams.isEmpty) {
            logger.info("Writing " + psmParamParams.size + " category portion size method parameters")
            BatchSql(categoriesInsertPsmParamsQuery, psmParamParams).execute()
          } else
            logger.warn("No category portion size method parameters found")
        } else
          logger.warn("No category portion size method records found")

        conn.commit()
        Right(())
      } else {
        logger.warn("Categories file contains no records")
        Right(())
      }
  }

  def deleteCategory(categoryCode: String): Either[CategoryCodeError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM categories WHERE code={category_code}").on('category_code -> categoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(UndefinedCode)
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM foods_categories WHERE food_code={food_code} AND category_code={category_code}").on('category_code -> categoryCode, 'food_code -> foodCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(UndefinedCode)
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>

      val rowsAffected = SQL("DELETE FROM categories_categories WHERE subcategory_code={subcategory_code} AND category_code={category_code}")
        .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(UndefinedCode)
  }
}
