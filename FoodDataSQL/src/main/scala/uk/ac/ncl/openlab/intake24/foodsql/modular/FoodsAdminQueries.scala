package uk.ac.ncl.openlab.intake24.foodsql.modular

import java.util.UUID

import anorm.NamedParameter.symbol
import anorm.{AnormUtil, Macro, NamedParameter, SQL, SqlParser, sqlToSimple}
import org.apache.commons.lang3.StringUtils
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24._
import uk.ac.ncl.openlab.intake24.api.data.InheritableAttributes
import uk.ac.ncl.openlab.intake24.api.data.admin.{LocalFoodRecordUpdate, MainFoodRecordUpdate, NewLocalFoodRecord, NewMainFoodRecord}
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.foodsql.shared.FoodPortionSizeShared
import uk.ac.ncl.openlab.intake24.foodsql.{FirstRowValidation, FirstRowValidationClause}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

trait FoodsAdminQueries extends FoodsAdminService
  with SqlDataService
  with SqlResourceLoader
  with FirstRowValidation
  with FoodPortionSizeShared {

  private val logger = LoggerFactory.getLogger(classOf[FoodsAdminQueries])

  private case class FoodRow(code: String, description: String, local_description: Option[String], food_group_id: Long)

  private val foodRowParser = Macro.namedParser[FoodRow]

  private case class FoodResultRow(version: UUID, code: String, description: String, local_description: Option[String], do_not_use: Option[Boolean], food_group_id: Long,
                                   same_as_before_option: Option[Boolean], ready_meal_option: Option[Boolean], reasonable_amount: Option[Int], local_version: Option[UUID])

  private case class NutrientTableRow(nutrient_table_id: String, nutrient_table_record_id: String)

  private lazy val foodNutrientTableCodesQuery = sqlFromResource("admin/food_nutrient_table_codes.sql")

  def getFoodNutrientTableCodesQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Map[String, String]] = {
    val nutrientTableCodesResult = SQL(foodNutrientTableCodesQuery).on('food_code -> code, 'locale_id -> locale).executeQuery()

    val parsed = parseWithLocaleAndFoodValidation(code, nutrientTableCodesResult, Macro.namedParser[NutrientTableRow].+)(Seq(FirstRowValidationClause("nutrient_table_id", () => Right(List()))))

    parsed.right.map {
      _.map {
        case NutrientTableRow(id, code) => (id -> code)
      }.toMap
    }
  }

  private lazy val foodRecordQuery = sqlFromResource("admin/food_record.sql")

  private val foodInsertQuery = "INSERT INTO foods VALUES ({code}, {description}, {food_group_id}, {version}::uuid)"

  private val foodAttributesInsertQuery = "INSERT INTO foods_attributes VALUES (DEFAULT, {food_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount}, {use_in_recipes})"

  private def truncateDescription(description: String, foodCode: String) = {
    if (description.length() > 128) {
      logger.warn(s"Description too long for food ${foodCode}, truncating:")
      logger.warn(description)
      description.take(128)
    } else
      description

  }

  protected def createFoodsQuery(foods: Seq[NewMainFoodRecord])(implicit conn: java.sql.Connection): Either[DependentCreateError, Unit] = {
    if (foods.nonEmpty) {
      logger.debug(s"Writing ${foods.size} new food records to database")

      val errors = Map("food_group_id_fk" -> (e => ParentRecordNotFound(e)), "foods_code_pk" -> (e => DuplicateCode(e)))

      tryWithConstraintsCheck(errors) {
        val foodParams = foods.map {
          f =>
            Seq[NamedParameter]('code -> f.code, 'description -> truncateDescription(f.englishDescription, f.code), 'food_group_id -> f.groupCode, 'version -> UUID.randomUUID())
        }

        batchSql(foodInsertQuery, foodParams).execute()

        val foodAttributeParams =
          foods.map(f => Seq[NamedParameter]('food_code -> f.code, 'same_as_before_option -> f.attributes.sameAsBeforeOption,
            'ready_meal_option -> f.attributes.readyMealOption, 'reasonable_amount -> f.attributes.reasonableAmount,
            'use_in_recipes -> f.attributes.useInRecipes))

        batchSql(foodAttributesInsertQuery, foodAttributeParams).execute()

        val localeRestrictionParams = foods.flatMap {
          f =>
            f.localeRestrictions.map {
              locale =>
                Seq[NamedParameter]('food_code -> f.code, 'locale_id -> locale)
            }
        }

        if (localeRestrictionParams.nonEmpty) {
          batchSql("INSERT INTO foods_restrictions VALUES({food_code},{locale_id})", localeRestrictionParams).execute()
        }

        Right(())
      }
    } else {
      logger.debug("Create foods request with empty foods list")
      Right(())
    }
  }

  private def mkBatchCategoriesMap(foods: Seq[NewMainFoodRecord]) = {
    val z = Map[String, Seq[String]]()
    foods.foldLeft(z) {
      (map, food) => map + (food.code -> food.parentCategories)
    }
  }

  private val foodLocalInsertQuery = "INSERT INTO foods_local VALUES({food_code}, {locale_id}, {local_description}, {simple_local_description}, {do_not_use}, {version}::uuid)"

  private val foodNutrientMappingInsertQuery = "INSERT INTO foods_nutrient_mapping VALUES ({food_code}, {locale_id}, {nutrient_table_id}, {nutrient_table_code})"

  private val foodPsmInsertQuery =
    """INSERT INTO foods_portion_size_methods(food_code,locale_id,method,description,image_url,use_for_recipes,conversion_factor)
      |VALUES({food_code},{locale_id},{method},{description},{image_url},{use_for_recipes},{conversion_factor})""".stripMargin

  private val foodPsmParamsInsertQuery = "INSERT INTO foods_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})"

  protected def createLocalFoodsQuery(localFoodRecords: Map[String, NewLocalFoodRecord], locale: String)(implicit conn: java.sql.Connection): Either[Nothing, Unit] = {
    if (localFoodRecords.nonEmpty) {

      val localFoodRecordsSeq = localFoodRecords.toSeq

      logger.debug(s"Writing ${localFoodRecordsSeq.size} new local food records to database")

      val foodLocalParams = localFoodRecordsSeq.map {
        case (code, local) =>
          Seq[NamedParameter]('food_code -> code, 'locale_id -> locale, 'local_description -> local.localDescription.map(d => truncateDescription(d, code)),
            'simple_local_description -> local.localDescription.map(d => StringUtils.stripAccents(truncateDescription(d, code))),
            'do_not_use -> local.doNotUse, 'version -> UUID.randomUUID())
      }.toSeq

      batchSql(foodLocalInsertQuery, foodLocalParams).execute()

      val foodNutritionTableParams =
        localFoodRecordsSeq.flatMap {
          case (code, local) =>
            local.nutrientTableCodes.map {
              case (table_id, table_code) => Seq[NamedParameter]('food_code -> code, 'locale_id -> locale, 'nutrient_table_id -> table_id, 'nutrient_table_code -> table_code)
            }
        }.toSeq

      if (foodNutritionTableParams.nonEmpty)
        batchSql(foodNutrientMappingInsertQuery, foodNutritionTableParams).execute()

      val psmParams =
        localFoodRecordsSeq.flatMap {
          case (code, local) =>
            local.portionSize.map(ps => Seq[NamedParameter]('food_code -> code, 'locale_id -> locale, 'method -> ps.method,
              'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes, 'conversion_factor -> ps.conversionFactor))
        }.toSeq

      if (psmParams.nonEmpty) {
        val ids = AnormUtil.batchKeys(batchSql(foodPsmInsertQuery, psmParams))

        val psmParamParams = localFoodRecordsSeq.flatMap(_._2.portionSize).zip(ids).flatMap {
          case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
        }

        if (psmParamParams.nonEmpty)
          batchSql(foodPsmParamsInsertQuery, psmParamParams).execute()
      }

      Right(())

    } else {
      logger.debug("Create local foods request with empty foods list")
      Right(())
    }

  }

  protected def getFoodLocaleRestrictionsQuery(foodCode: String)(implicit conn: java.sql.Connection): Either[UnexpectedDatabaseError, Seq[String]] = {
    Right(SQL("SELECT locale_id FROM foods_restrictions WHERE food_code={food_code}").on('food_code -> foodCode).as(SqlParser.str("locale_id").*))
  }

  protected def updateFoodLocaleRestrictionsQuery(foodCode: String, localeRestrictions: Seq[String])(implicit conn: java.sql.Connection): Either[LocalLookupError, Unit] = {

    val errors = Map[String, PSQLException => LocalLookupError]("foods_restrictions_food_code_fk" -> (e => RecordNotFound(new RuntimeException(foodCode))), "foods_restrictions_locale_id_fk" -> (e => UndefinedLocale(e)))

    tryWithConstraintsCheck(errors) {
      SQL("DELETE FROM foods_restrictions WHERE food_code={food_code}").on('food_code -> foodCode).execute()

      if (localeRestrictions.nonEmpty) {
        val params = localeRestrictions.map(locale => Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale))
        batchSql("INSERT INTO foods_restrictions VALUES({food_code},{locale_id})", params).execute()
      }

      Right(())
    }
  }

  protected def updateFoodAttributesQuery(foodCode: String, attributes: InheritableAttributes)(implicit conn: java.sql.Connection): Either[LookupError, Unit] = {
    tryWithConstraintCheck("foods_attributes_food_code_fk", e => RecordNotFound(new RuntimeException(foodCode))) {
      SQL("DELETE FROM foods_attributes WHERE food_code={food_code}").on('food_code -> foodCode).execute()

      SQL(foodAttributesInsertQuery)
        .on('food_code -> foodCode, 'same_as_before_option -> attributes.sameAsBeforeOption,
          'ready_meal_option -> attributes.readyMealOption, 'reasonable_amount -> attributes.reasonableAmount,
          'use_in_recipes -> attributes.useInRecipes).execute()

      Right(())
    }
  }

  protected def updateFoodQuery(foodCode: String, foodRecord: MainFoodRecordUpdate)(implicit conn: java.sql.Connection): Either[UpdateError, Unit] = {
    val rowsAffected = SQL("UPDATE foods SET code = {new_code}, description={description}, food_group_id={food_group_id}, version={new_version}::uuid WHERE code={food_code} AND version={base_version}::uuid")
      .on('food_code -> foodCode, 'base_version -> foodRecord.baseVersion,
        'new_version -> UUID.randomUUID(), 'new_code -> foodRecord.code, 'description -> foodRecord.englishDescription, 'food_group_id -> foodRecord.groupCode)
      .executeUpdate()

    if (rowsAffected == 1) {
      Right(())
    } else
      Left(VersionConflict(new RuntimeException(foodCode)))
  }

  protected def updateLocalFoodQuery(foodCode: String, foodLocal: LocalFoodRecordUpdate, locale: String)(implicit conn: java.sql.Connection): Either[LocalDependentUpdateError, Unit] = {
    try {
      SQL("DELETE FROM foods_nutrient_mapping WHERE food_code={food_code} AND locale_id={locale_id}")
        .on('food_code -> foodCode, 'locale_id -> locale).execute()

      SQL("DELETE FROM foods_portion_size_methods WHERE food_code={food_code} AND locale_id={locale_id}")
        .on('food_code -> foodCode, 'locale_id -> locale).execute()

      if (foodLocal.nutrientTableCodes.nonEmpty) {
        val nutrientTableCodesParams = foodLocal.nutrientTableCodes.map {
          case (table_id, table_code) => Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale, 'nutrient_table_id -> table_id, 'nutrient_table_code -> table_code)
        }.toSeq

        batchSql(foodNutrientMappingInsertQuery, nutrientTableCodesParams).execute()
      }

      if (foodLocal.portionSize.nonEmpty) {
        val psmParams = foodLocal.portionSize.map(ps => Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description,
          'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes, 'conversion_factor -> ps.conversionFactor))

        val psmKeys = AnormUtil.batchKeys(batchSql(foodPsmInsertQuery, psmParams))

        val psmParamParams = foodLocal.portionSize.zip(psmKeys).flatMap {
          case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
        }

        if (psmParamParams.nonEmpty)
          batchSql(foodPsmParamsInsertQuery, psmParamParams).execute()
      }

      foodLocal.baseVersion match {
        case Some(version) => {

          val rowsAffected = SQL("UPDATE foods_local SET version = {new_version}::uuid, local_description = {local_description}, simple_local_description = {simple_local_description}, do_not_use = {do_not_use} WHERE food_code = {food_code} AND locale_id = {locale_id} AND version = {base_version}::uuid")
            .on('food_code -> foodCode, 'locale_id -> locale, 'base_version -> foodLocal.baseVersion, 'new_version -> UUID.randomUUID(),
              'local_description -> foodLocal.localDescription.map(d => truncateDescription(d, foodCode)), 'simple_local_description -> foodLocal.localDescription.map(d => truncateDescription(StringUtils.stripAccents(d), foodCode)), 'do_not_use -> foodLocal.doNotUse)
            .executeUpdate()

          if (rowsAffected == 1) {
            Right(())
          } else
            Left(VersionConflict(new RuntimeException(foodCode)))
        }
        case None => {
          try {
            SQL(foodLocalInsertQuery)
              .on('food_code -> foodCode, 'locale_id -> locale, 'local_description -> foodLocal.localDescription.map(d => truncateDescription(d, foodCode)), 'simple_local_description -> foodLocal.localDescription.map(d => truncateDescription(StringUtils.stripAccents(d), foodCode)),
                'do_not_use -> foodLocal.doNotUse, 'version -> UUID.randomUUID())
              .execute()

            Right(())
          } catch {
            case e: PSQLException =>
              if (e.getServerErrorMessage.getConstraint == "foods_local_pk") {
                Left(VersionConflict(new RuntimeException(foodCode)))
              } else
                throw e
          }
        }
      }

    } catch {
      case e: PSQLException => {
        e.getServerErrorMessage.getConstraint match {
          case "foods_nutrient_tables_food_code_fk" | "foods_portion_size_methods_food_id_fk" | "foods_local_food_code_fk" => Left(RecordNotFound(new RuntimeException(foodCode)))
          case _ => throw e
        }
      }
    }
  }
}
