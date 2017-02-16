package uk.ac.ncl.openlab.intake24.foodsql.modular

import java.sql.BatchUpdateException
import java.util.UUID

import anorm.NamedParameter.symbol
import anorm._
import org.apache.commons.lang3.StringUtils
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.foodsql.shared.FoodPortionSizeShared
import uk.ac.ncl.openlab.intake24.foodsql.{FirstRowValidation, FirstRowValidationClause}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

trait CategoriesAdminQueries
  extends SqlDataService
    with SqlResourceLoader
    with FoodPortionSizeShared
    with FirstRowValidation {

  private val logger = LoggerFactory.getLogger(classOf[CategoriesAdminQueries])

  private lazy val categoryPsmQuery = sqlFromResource("shared/category_portion_size_methods.sql")

  protected def categoryPortionSizeMethodsQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[PortionSizeMethod]] = {
    val psmResults = SQL(categoryPsmQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, psmResults, psmResultRowParser.+)(Seq(FirstRowValidationClause("id", () => Right(List())))).right.map(mkPortionSizeMethods)
  }

  private case class CategoryResultRow(version: UUID, local_version: Option[UUID], code: String, description: String, local_description: Option[String], is_hidden: Boolean, same_as_before_option: Option[Boolean],
                                       ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  protected def updateCategoryAttributesQuery(categoryCode: String, attributes: InheritableAttributes)(implicit conn: java.sql.Connection): Either[UpdateError, Unit] = {
    try {
      SQL("DELETE FROM categories_attributes WHERE category_code={category_code}")
        .on('category_code -> categoryCode).execute()

      SQL("INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})")
        .on('category_code -> categoryCode, 'same_as_before_option -> attributes.sameAsBeforeOption,
          'ready_meal_option -> attributes.readyMealOption, 'reasonable_amount -> attributes.reasonableAmount).execute()

      Right(())
    } catch {
      case e: PSQLException => {
        e.getServerErrorMessage.getConstraint match {
          case "categories_attributes_category_code_fk" => Left(RecordNotFound(new RuntimeException(categoryCode)))
          case _ => throw e
        }
      }
    }
  }

  protected def updateCategoryQuery(categoryCode: String, record: MainCategoryRecordUpdate)(implicit conn: java.sql.Connection): Either[UpdateError, Unit] = {
    val rowsAffected = SQL("UPDATE categories SET code = {new_code}, description={description}, is_hidden={is_hidden}, version={new_version}::uuid WHERE code={category_code} AND version={base_version}::uuid")
      .on('category_code -> categoryCode, 'base_version -> record.baseVersion,
        'new_version -> UUID.randomUUID(), 'new_code -> record.code, 'description -> record.englishDescription, 'is_hidden -> record.isHidden)
      .executeUpdate()

    if (rowsAffected == 1) {
      Right(())
    } else {
      Left(VersionConflict(new RuntimeException(categoryCode)))
    }
  }

  private val categoriesInsertPsmParamsQuery = "INSERT INTO categories_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})"

  private val categoriesInsertLocalQuery = "INSERT INTO categories_local VALUES({category_code}, {locale_id}, {local_description}, {simple_local_description}, {version}::uuid)"

  private val categoriesPsmInsertQuery = "INSERT INTO categories_portion_size_methods VALUES(DEFAULT, {category_code}, {locale_id}, {method}, {description}, {image_url}, {use_for_recipes})"

  protected def updateCategoryPortionSizeMethodsQuery(categoryCode: String, portionSize: Seq[PortionSizeMethod], locale: String)(implicit conn: java.sql.Connection): Either[LocalUpdateError, Unit] = {
    val errors = Map[String, PSQLException => LocalUpdateError]("categories_portion_size_methods_categories_code_fk" -> (e => RecordNotFound(new RuntimeException(categoryCode))),
      "categories_portion_size_methods_locale_id_fk" -> (e => UndefinedLocale(e)))

    SQL("DELETE FROM categories_portion_size_methods WHERE category_code={category_code} AND locale_id={locale_id}")
      .on('category_code -> categoryCode, 'locale_id -> locale).execute()

    if (portionSize.nonEmpty) {
      tryWithConstraintsCheck(errors) {
        val psmParams = portionSize.map(ps => Seq[NamedParameter]('category_code -> categoryCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))

        val psmKeys = AnormUtil.batchKeys(batchSql(categoriesPsmInsertQuery, psmParams))

        val psmParamParams = portionSize.zip(psmKeys).flatMap {
          case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
        }

        if (psmParamParams.nonEmpty)
          batchSql(categoriesInsertPsmParamsQuery, psmParamParams).execute()

        Right(())
      }
    } else Right(())
  }

  protected def updateCategoryLocalQuery(categoryCode: String, categoryLocal: LocalCategoryRecordUpdate, locale: String)(implicit conn: java.sql.Connection): Either[LocalDependentUpdateError, Unit] = {
    val errors = Map[String, PSQLException => LocalDependentUpdateError](
      "categories_local_pk" -> (e => DuplicateCode(e)),
      "categories_local_category_code_fk" -> (e => ParentRecordNotFound(e)),
      "categories_local_locale_id_fk" -> (e => UndefinedLocale(e)))

    tryWithConstraintsCheck(errors) {
      val rowsAffected = SQL("UPDATE categories_local SET version = {new_version}::uuid, local_description = {local_description}, simple_local_description = {simple_local_description} WHERE category_code = {category_code} AND locale_id = {locale_id} AND version = {base_version}::uuid")
        .on('category_code -> categoryCode, 'locale_id -> locale, 'base_version -> categoryLocal.baseVersion, 'new_version -> UUID.randomUUID(), 'local_description -> categoryLocal.localDescription,
          'simple_local_description -> categoryLocal.localDescription.map(s => StringUtils.stripAccents(s)))
        .executeUpdate()

      if (rowsAffected == 1) {
        Right(())
      } else {
        Left(VersionConflict(new RuntimeException(categoryCode)))
      }
    }
  }

  protected def createCategoryLocalQuery(categoryCode: String, locale: String, categoryLocal: NewLocalCategoryRecord)(implicit conn: java.sql.Connection): Either[LocalDependentCreateError, Unit] = {

    val errors = Map("categories_local_pk" -> DuplicateCode, "categories_local_category_code_fk" -> ParentRecordNotFound, "categories_local_locale_id_fk" -> UndefinedLocale)

    tryWithConstraintsCheck(errors) {
      SQL(categoriesInsertLocalQuery)
        .on('category_code -> categoryCode, 'locale_id -> locale, 'local_description -> categoryLocal.localDescription,
          'simple_local_description -> categoryLocal.localDescription.map(s => StringUtils.stripAccents(s)), 'version -> UUID.randomUUID())
        .execute()
      Right(())
    }
  }

  protected def removeFoodFromAllCategoriesQuery(foodCode: String)(implicit conn: java.sql.Connection): Either[Nothing, Unit] = {
    SQL("DELETE FROM foods_categories WHERE food_code={food_code}").on('food_code -> foodCode).execute()
    Right(())
  }

  private val foodsCategoriesInsertQuery = "INSERT INTO foods_categories VALUES(DEFAULT, {food_code},{category_code})"

  protected def addFoodsToCategoriesQuery(categoryFoods: Map[String, Seq[String]])(implicit conn: java.sql.Connection): Either[ParentError, Unit] = {
    try {
      val foodCategoryParams =
        categoryFoods.flatMap {
          case (foodCode, categories) =>
            categories.map(categoryCode => Seq[NamedParameter]('food_code -> foodCode, 'category_code -> categoryCode))
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
          case "foods_categories_category_code_fk" => Left(ParentRecordNotFound(e2))
          case "foods_categories_food_code_fk" => Left(ParentRecordNotFound(e2))
          case _ => throw e
        }
        case _ => throw e
      }
    }
  }

  private val categoriesCategoriesInsertQuery = "INSERT INTO categories_categories VALUES(DEFAULT, {subcategory_code},{category_code})"

  protected def removeSubcategoryFromAllCategoriesQuery(subcategoryCode: String)(implicit conn: java.sql.Connection): Either[Nothing, Unit] = {
    SQL("DELETE FROM categories_categories WHERE subcategory_code={subcategory_code}").on('subcategory_code -> subcategoryCode).execute()
    Right(())
  }

  protected def addSubcategoriesToCategoriesQuery(categorySubcategories: Map[String, Seq[String]])(implicit conn: java.sql.Connection): Either[ParentError, Unit] = {
    try {
      categorySubcategories.find {
        case (code, subcats) => subcats.contains(code)
      } match {
        case Some((code, _)) => Left(IllegalParent(new RuntimeException(s"Cannot add category $code to itself")))
        case None => {

          val categoryCategoryParams =
            categorySubcategories.flatMap {
              case (subcategoryCode, categories) =>
                categories.map(categoryCode => Seq[NamedParameter]('subcategory_code -> subcategoryCode, 'category_code -> categoryCode))
            }.toSeq

          if (!categoryCategoryParams.isEmpty) {
            logger.debug("Writing " + categoryCategoryParams.size + " category parent category records")
            batchSql(categoriesCategoriesInsertQuery, categoryCategoryParams).execute()
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
          case "categories_categories_subcategory_code_fk" => Left(ParentRecordNotFound(e))
          case "categories_categories_category_code_fk" => Left(ParentRecordNotFound(e))
          case _ => throw e
        }
    }
  }

  private val categoriesInsertQuery = "INSERT INTO categories VALUES({code},{description},{is_hidden},{version}::uuid)"

  private val categoriesAttributesInsertQuery = "INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})"

  protected def createCategoriesQuery(categories: Seq[NewCategory])(implicit conn: java.sql.Connection): Either[CreateError, Unit] = {
    if (!categories.isEmpty) {
      logger.debug("Writing " + categories.size + " categories to database")

      val categoryParams =
        categories.map(c => Seq[NamedParameter]('code -> c.code, 'description -> c.englishDescription, 'is_hidden -> c.isHidden, 'version -> UUID.randomUUID()))

      tryWithConstraintCheck("categories_pk", DuplicateCode) {
        batchSql(categoriesInsertQuery, categoryParams).execute()

        val categoryAttributeParams =
          categories.map(c => Seq[NamedParameter]('category_code -> c.code, 'same_as_before_option -> c.attributes.sameAsBeforeOption,
            'ready_meal_option -> c.attributes.readyMealOption, 'reasonable_amount -> c.attributes.reasonableAmount))

        batchSql(categoriesAttributesInsertQuery, categoryAttributeParams).execute()

        Right(())
      }
    } else {
      logger.debug("Create categories request with empty foods list")
      Right(())
    }
  }

  protected def createLocalCategoriesQuery(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String)(implicit conn: java.sql.Connection): Either[LocalCreateError, Unit] = {
    if (localCategoryRecords.nonEmpty) {

      val localCategoryRecordsSeq = localCategoryRecords.toSeq

      val errors = Map[String, PSQLException => LocalCreateError]("categories_local_locale_id_fk" -> (e => UndefinedLocale(e)), "categories_local_pk" -> (e => DuplicateCode(e)))

      logger.debug(s"Writing ${localCategoryRecordsSeq.size} new local category records to database")

      tryWithConstraintsCheck(errors) {

        val localCategoryParams =
          localCategoryRecordsSeq.map {
            case (code, local) =>
              Seq[NamedParameter]('category_code -> code, 'locale_id -> locale, 'local_description -> local.localDescription,
                'simple_local_description -> local.localDescription.map(StringUtils.stripAccents(_)), 'version -> Some(UUID.randomUUID()))
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

          val keys = AnormUtil.batchKeys(batchSql(categoriesPsmInsertQuery, psmParams))

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

        Right(())
      }
    } else {
      logger.debug("Categories file contains no records")
      Right(())
    }
  }
}
