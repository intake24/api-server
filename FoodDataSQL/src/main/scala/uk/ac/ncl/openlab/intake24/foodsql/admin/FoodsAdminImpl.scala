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
import uk.ac.ncl.openlab.intake24.NewFood
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
import javax.sql.DataSource
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordType
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesImpl
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.LocalFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale

class FoodsAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends FoodsAdminImpl

trait FoodsAdminImpl extends FoodsAdminService
    with SqlDataService
    with SqlResourceLoader
    with FirstRowValidation
    with FoodPortionSizeShared
    with AdminErrorMessagesShared
    with BrandNamesAdminImpl
    with FoodBrowsingAdminImpl
    with AssociatedFoodsAdminImpl
    with CategoriesAdminImpl {

  private val logger = LoggerFactory.getLogger(classOf[FoodsAdminImpl])

  private def foodCodePkConstraintFailedMessage(foodCode: String) =
    s"Food code $foodCode already exists. Duplicate food codes are not allowed."

  private val foodCodePkConstraintFailedCode = "duplicate_food_code"

  private val temporaryCodesExhausted = "temporary_codes_exhausted"

  private val temporaryCodesExhaustedMessage = "Cannot assign a temporary food code, tried F000 through F999 but none are available."

  /*  val foodsDeleteVersioned = """DELETE FROM foods WHERE code={food_code} AND version={version}::uuid"""

  val foodsLocalDelete = """DELETE FROM foods_local WHERE food_code={food_code} AND locale_id={locale_id}""" */

  private case class FoodRow(code: String, description: String, local_description: Option[String], food_group_id: Long)

  private val foodRowParser = Macro.namedParser[FoodRow]

  private case class FoodResultRow(version: UUID, code: String, description: String, local_description: Option[String], do_not_use: Option[Boolean], food_group_id: Long,
    same_as_before_option: Option[Boolean], ready_meal_option: Option[Boolean], reasonable_amount: Option[Int], local_version: Option[UUID])

  private case class NutrientTableRow(nutrient_table_id: String, nutrient_table_record_id: String)

  private lazy val foodNutrientTableCodesQuery = sqlFromResource("admin/food_nutrient_table_codes.sql")

  def foodNutrientTableCodes(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Map[String, String]] = {
    val nutrientTableCodesResult = SQL(foodNutrientTableCodesQuery).on('food_code -> code, 'locale_id -> locale).executeQuery()

    val parsed = parseWithLocaleAndFoodValidation(code, nutrientTableCodesResult, Macro.namedParser[NutrientTableRow].+)(Seq(FirstRowValidationClause("nutrient_table_id", Right(List()))))

    parsed.right.map {
      _.map {
        case NutrientTableRow(id, code) => (id -> code)
      }.toMap
    }
  }

  private lazy val foodRecordQuery = sqlFromResource("admin/food_record.sql")

  def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord] = tryWithConnection {
    implicit conn =>
      for (
        nutrientTableCodes <- foodNutrientTableCodes(code, locale).right;
        portionSizeMethods <- foodPortionSizeMethodsImpl(code, locale).right;
        associatedFoods <- getAssociatedFoodsWithHeaders(code, locale).right;
        parentCategories <- getFoodParentCategories(code, locale).right;
        brandNames <- getBrandNames(code, locale).right;
        record <- {
          val foodQueryResult = SQL(foodRecordQuery).on('food_code -> code, 'locale_id -> locale).executeQuery()

          parseWithLocaleAndFoodValidation(code, foodQueryResult, Macro.namedParser[FoodResultRow].single)().right.map {
            result =>
              FoodRecord(
                MainFoodRecord(result.version, result.code, result.description, result.food_group_id.toInt,
                  InheritableAttributes(result.ready_meal_option, result.same_as_before_option, result.reasonable_amount), parentCategories),
                LocalFoodRecord(result.local_version, result.local_description, result.do_not_use.getOrElse(false), nutrientTableCodes, portionSizeMethods,
                  associatedFoods, brandNames))
          }
        }.right
      ) yield record
  }

  private val foodInsertQuery = "INSERT INTO foods VALUES ({code}, {description}, {food_group_id}, {version}::uuid)"

  private val foodAttributesInsertQuery = "INSERT INTO foods_attributes VALUES (DEFAULT, {food_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})"

  private def truncateDescription(description: String, foodCode: String) = {
    if (description.length() > 128) {
      logger.warn(s"Description too long for food ${foodCode}, truncating:")
      logger.warn(description)
      description.take(128)
    } else
      description

  }

  def isFoodCode(code: String): Either[DatabaseError, Boolean] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT code FROM foods WHERE code={food_code}""").on('food_code -> code).executeQuery().as(SqlParser.str("code").*).nonEmpty)
  }

  def isFoodCodeAvailable(code: String): Either[DatabaseError, Boolean] = isFoodCode(code).right.map(!_)

  def createNewFoodComposable(newFood: NewFood)(implicit conn: java.sql.Connection): Either[DependentCreateError, Unit] = {
    val errors = Map("food_group_id_fk" -> ParentRecordNotFound, "foods_code_pk" -> DuplicateCode)

    tryWithConstraintsCheck[DependentCreateError, Unit](errors) {
      SQL(foodInsertQuery)
        .on('code -> newFood.code, 'description -> newFood.englishDescription, 'food_group_id -> newFood.groupCode, 'version -> UUID.randomUUID())
        .execute()

      SQL(foodAttributesInsertQuery)
        .on('food_code -> newFood.code, 'same_as_before_option -> newFood.attributes.sameAsBeforeOption,
          'ready_meal_option -> newFood.attributes.readyMealOption, 'reasonable_amount -> newFood.attributes.reasonableAmount).execute()

      Right(())
    }
  }

  def createFood(newFood: NewFood): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        createNewFoodComposable(newFood).right.flatMap {
          _ =>
            addFoodsToCategoriesComposable(Map(newFood.code -> newFood.parentCategories)).left.map {
              case RecordNotFound => ParentRecordNotFound
              case e: DatabaseError => e
            }
        }
      }
  }

  def createFoodWithTempCode(newFood: NewFood): Either[DependentCreateError, String] = {
    def tryNextNumber(n: Int): Either[DependentCreateError, String] = {
      if (n > 999)
        Left(DuplicateCode)
      else {
        val tempCode = "F%03d".format(n)
        createFood(newFood.copy(code = tempCode)) match {
          case Right(()) => Right(tempCode)
          case Left(DuplicateCode) => tryNextNumber(n + 1)
          case Left(x) => Left(x)
        }
      }
    }

    tryNextNumber(0)
  }

  def createFoodsComposable(foods: Seq[NewFood])(implicit conn: java.sql.Connection): Either[DependentCreateError, Unit] = {
    if (foods.nonEmpty) {
      logger.info(s"Writing ${foods.size} new food records to database")

      val errors = Map("food_group_id_fk" -> ParentRecordNotFound, "foods_code_pk" -> DuplicateCode)

      tryWithConstraintsCheck(errors) {
        val foodParams = foods.map {
          f =>
            Seq[NamedParameter]('code -> f.code, 'description -> truncateDescription(f.englishDescription, f.code), 'food_group_id -> f.groupCode, 'version -> UUID.randomUUID())
        }

        batchSql(foodInsertQuery, foodParams).execute()

        val foodAttributeParams =
          foods.map(f => Seq[NamedParameter]('food_code -> f.code, 'same_as_before_option -> f.attributes.sameAsBeforeOption,
            'ready_meal_option -> f.attributes.readyMealOption, 'reasonable_amount -> f.attributes.reasonableAmount))

        batchSql(foodAttributesInsertQuery, foodAttributeParams).execute()

        Right(())
      }
    } else {
      logger.warn("Create foods request with empty foods list")
      Right(())
    }
  }

  private def mkBatchCategoriesMap(foods: Seq[NewFood]) = {
    val z = Map[String, Seq[String]]()
    foods.foldLeft(z) {
      (map, food) => map + (food.code -> food.parentCategories)
    }
  }

  def createFoods(foods: Seq[NewFood]): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        createFoodsComposable(foods).right.flatMap {
          _ =>
            addFoodsToCategoriesComposable(mkBatchCategoriesMap(foods)).left.map {
              case RecordNotFound => ParentRecordNotFound
              case e: DatabaseError => e
            }
        }
      }
  }

  def deleteAllFoods(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM foods").execute()
      Right(())
  }

  def deleteFood(foodCode: String): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("""DELETE FROM foods WHERE code={food_code}""").on('food_code -> foodCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(RecordNotFound)
  }

  private val foodLocalInsertQuery = "INSERT INTO foods_local VALUES({food_code}, {locale_id}, {local_description}, {do_not_use}, {version}::uuid)"

  private val foodNutrientMappingInsertQuery = "INSERT INTO foods_nutrient_mapping VALUES ({food_code}, {locale_id}, {nutrient_table_id}, {nutrient_table_code})"

  private val foodPsmInsertQuery = "INSERT INTO foods_portion_size_methods VALUES(DEFAULT, {food_code}, {locale_id}, {method}, {description}, {image_url}, {use_for_recipes})"

  private val foodPsmParamsInsertQuery = "INSERT INTO foods_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})"

  def createLocalFoodsComposable(localFoodRecords: Map[String, NewLocalFoodRecord], locale: String)(implicit conn: java.sql.Connection): Either[DatabaseError, Unit] = {
    if (localFoodRecords.nonEmpty) {

      val localFoodRecordsSeq = localFoodRecords.toSeq

      logger.info(s"Writing ${localFoodRecordsSeq.size} new local food records to database")

      val foodLocalParams = localFoodRecordsSeq.map {
        case (code, local) =>
          Seq[NamedParameter]('food_code -> code, 'locale_id -> locale, 'local_description -> local.localDescription.map(d => truncateDescription(d, code)),
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
            local.portionSize.map(ps => Seq[NamedParameter]('food_code -> code, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))
        }.toSeq

      if (psmParams.nonEmpty) {
        val ids = Util.batchKeys(batchSql(foodPsmInsertQuery, psmParams))

        val psmParamParams = localFoodRecordsSeq.flatMap(_._2.portionSize).zip(ids).flatMap {
          case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
        }

        if (psmParamParams.nonEmpty)
          batchSql(foodPsmParamsInsertQuery, psmParamParams).execute()
      }

      Right(())

    } else {
      logger.warn("Create local foods request with empty foods list")
      Right(())
    }

  }

  def createLocalFoods(localFoodRecords: Map[String, NewLocalFoodRecord], locale: String): Either[LocalDependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- createLocalFoodsComposable(localFoodRecords, locale).right;
          _ <- createAssociatedFoodsComposable(localFoodRecords.mapValues(_.associatedFoods), locale).right;
          _ <- createBrandNamesComposable(localFoodRecords.mapValues(_.brandNames), locale).right
        ) yield ()
      }
  }

  def updateFoodAttributesComposable(foodCode: String, attributes: InheritableAttributes)(implicit conn: java.sql.Connection): Either[LookupError, Unit] = {
    try {
      SQL("DELETE FROM foods_attributes WHERE food_code={food_code}").on('food_code -> foodCode).execute()

      SQL(foodAttributesInsertQuery)
        .on('food_code -> foodCode, 'same_as_before_option -> attributes.sameAsBeforeOption,
          'ready_meal_option -> attributes.readyMealOption, 'reasonable_amount -> attributes.reasonableAmount).execute()

      Right(())
    } catch {
      case e: PSQLException => {
        e.getServerErrorMessage.getConstraint match {
          case "foods_attributes_food_code_fk" => Left(RecordNotFound)
          case _ => throw e
        }
      }
    }
  }

  def updateFoodParentCategoriesComposable(foodCode: String, parentCategories: Seq[String])(implicit conn: java.sql.Connection): Either[LookupError, Unit] =
    for (
      _ <- removeFoodFromAllCategoriesComposable(foodCode).right;
      _ <- addFoodsToCategoriesComposable(Map(foodCode -> parentCategories)).right
    ) yield ()

  def updateMainFoodRecordFieldsComposable(foodCode: String, foodRecord: MainFoodRecordUpdate)(implicit conn: java.sql.Connection): Either[UpdateError, Unit] = {
    val rowsAffected = SQL("UPDATE foods SET code = {new_code}, description={description}, food_group_id={food_group_id}, version={new_version}::uuid WHERE code={food_code} AND version={base_version}::uuid)")
      .on('food_code -> foodCode, 'base_version -> foodRecord.version,
        'new_version -> UUID.randomUUID(), 'new_code -> foodRecord.code, 'description -> foodRecord.englishDescription, 'food_group_id -> foodRecord.groupCode)
      .executeUpdate()

    if (rowsAffected == 1) {
      Right(())
    } else
      Left(VersionConflict)
  }

  def updateMainFoodRecord(foodCode: String, foodRecord: MainFoodRecordUpdate): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- updateFoodAttributesComposable(foodCode, foodRecord.attributes).right;
          _ <- updateFoodParentCategoriesComposable(foodCode, foodRecord.parentCategories).right;
          _ <- updateMainFoodRecordFieldsComposable(foodCode, foodRecord).right
        ) yield ()
      }
  }

  def updateLocalFoodRecordFieldsComposable(foodCode: String, foodLocal: LocalFoodRecordUpdate, locale: String)(implicit conn: java.sql.Connection): Either[LocalDependentUpdateError, Unit] = {
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
        val psmParams = foodLocal.portionSize.map(ps => Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))

        val psmKeys = Util.batchKeys(batchSql(foodPsmInsertQuery, psmParams))

        val psmParamParams = foodLocal.portionSize.zip(psmKeys).flatMap {
          case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
        }

        if (psmParamParams.nonEmpty)
          batchSql(foodPsmParamsInsertQuery, psmParamParams).execute()
      }

      foodLocal.version match {
        case Some(version) => {

          val rowsAffected = SQL("UPDATE foods_local SET version = {new_version}::uuid, local_description = {local_description}, do_not_use = {do_not_use} WHERE food_code = {food_code} AND locale_id = {locale_id} AND version = {base_version}::uuid")
            .on('food_code -> foodCode, 'locale_id -> locale, 'base_version -> foodLocal.version, 'new_version -> UUID.randomUUID(), 'local_description -> foodLocal.localDescription, 'do_not_use -> foodLocal.doNotUse)
            .executeUpdate()

          if (rowsAffected == 1) {
            Right(())
          } else
            Left(VersionConflict)
        }
        case None => {
          try {
            SQL(foodLocalInsertQuery)
              .on('food_code -> foodCode, 'locale_id -> locale, 'local_description -> foodLocal.localDescription, 'do_not_use -> foodLocal.doNotUse, 'version -> UUID.randomUUID())
              .execute()

            Right(())
          } catch {
            case e: PSQLException =>
              if (e.getServerErrorMessage.getConstraint == "foods_local_pk") {
                Left(VersionConflict)
              } else
                throw e
          }
        }
      }

    } catch {
      case e: PSQLException => {
        e.getServerErrorMessage.getConstraint match {
          case "foods_nutrient_tables_food_code_fk" | "foods_portion_size_methods_food_id_fk" | "foods_local_food_code_fk" => Left(RecordNotFound)
          case _ => throw e
        }
      }
    }
  }

  def dependentToParentError(error: DependentCreateError): LocalDependentUpdateError = error match {
    case ParentRecordNotFound => RecordNotFound
    case DuplicateCode => DuplicateCode
    case e: DatabaseError => e
  }

  def localDependentToParentError(error: LocalDependentCreateError): LocalDependentUpdateError = error match {
    case ParentRecordNotFound => RecordNotFound
    case DuplicateCode => DuplicateCode
    case UndefinedLocale => UndefinedLocale
    case e: DatabaseError => e
  }

  def updateLocalFoodRecord(foodCode: String, foodLocal: LocalFoodRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- updateAssociatedFoodsComposable(foodCode, foodLocal.associatedFoods, locale).right;
          _ <- updateBrandNamesComposable(foodCode, foodLocal.brandNames, locale).left.map(e => localDependentToParentError(e)).right;
          _ <- updateLocalFoodRecordFieldsComposable(foodCode, foodLocal, locale).right
        ) yield ()
      }
  }

}
