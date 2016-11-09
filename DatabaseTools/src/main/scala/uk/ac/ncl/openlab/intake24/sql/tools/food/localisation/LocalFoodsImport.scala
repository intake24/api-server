package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.{AssociatedFood, Locale, NewLocalCategoryRecord, NewLocalFoodRecord}
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConnection, DatabaseOptions, ErrorHandler, WarningMessage}

sealed trait FoodCodingDecision

case object DoNotUse extends FoodCodingDecision

case class UseUKFoodTable(localDescription: String) extends FoodCodingDecision

case class UseLocalFoodTable(localDescription: String, localTableRecordId: String) extends FoodCodingDecision

case class NewFoodRecord(englishDescription: String, localDescription: String, localTableRecordId: String)

case class RecodingTable(existingFoodsCoding: Map[String, FoodCodingDecision], newFoods: Seq[NewFoodRecord])

case class LocalFoodsImport(localeCode: String, englishLocaleName: String, localLocaleName: String,
  respondentLanguageCode: String, adminLanguageCode: String, flagCode: String, localNutrientTableId: String,
  recodingTableParser: RecodingTableParser, psmTableParser: PortionSizeTableParser)
    extends App
    with NewLocalFoodsParser
    with AssociatedFoodTranslationParser
    with CategoryTranslationParser
    with RecodingTableUtil
    with WarningMessage
    with ErrorHandler
    with DatabaseConnection {

  val logger = LoggerFactory.getLogger(getClass)

  val baseLocaleCode = "en_GB"

  trait Options extends ScallopConf {
    version("Intake24 food localisation data import tool 16.9")

    val recodingTablePath = opt[String](required = true, noshort = true)
    val categoryTranslationPath = opt[String](required = true, noshort = true)
    val associatedFoodsTranslationPath = opt[String](required = true, noshort = true)
    val newLocalFoodsPath = opt[String](required = true, noshort = true)
    val psmTablePath = opt[String](required = true, noshort = true)
    val localFoodTablePath = opt[String](required = true, noshort = true)

    val logPath = opt[String](required = false, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage(s"WARNING: This will destroy all existing data for ${englishLocaleName} (${localeCode}) locale!")

  val dataSource = getDataSource(options)

  val dataService = new FoodDatabaseAdminImpl(dataSource)

  val indexDataService = new FoodIndexDataImpl(dataSource)

  logger.info(s"Loading $baseLocaleCode indexable foods")
  val indexableFoods = throwOnError(indexDataService.indexableFoods(baseLocaleCode))

  logger.info(s"Loading $baseLocaleCode indexable categories")
  val indexableCategories = throwOnError(indexDataService.indexableCategories(baseLocaleCode))

  // Parse spreadsheets

  logger.info(s"Loading $localeCode recoding table")
  val recodingTable = recodingTableParser.parseRecodingTable(options.recodingTablePath())

  logger.info(s"Loading $localeCode associated food prompts translations")
  val associatedFoodTranslations = parseAssociatedFoodTranslation(options.associatedFoodsTranslationPath()).map {
    case (foodCode, assocFoods) => foodCode -> assocFoods.map {
      v1 =>

        val isCategory = throwOnError(dataService.isCategoryCode(v1.category))

        val foodOrCategory = if (isCategory) {
          logger.debug(s"Resolved ${v1.category} as category code")
          Right(v1.category)
        } else {
          logger.debug(s"Resolved ${v1.category} as food code")
          Left(v1.category)
        }
        AssociatedFood(foodOrCategory, v1.promptText, v1.linkAsMain, v1.genericName)
    }
  }

  logger.info(s"Loading $localeCode new foods table")
  val (newFoods, newLocalFoodRecords) = buildNewLocalFoods(options.newLocalFoodsPath(), localeCode, localNutrientTableId, associatedFoodTranslations)
  
  logger.info(s"Loading $localeCode category translations")
  val categoryTranslations = parseCategoryTranslations(options.categoryTranslationPath())

  // Re-create locale
  
  logger.info(s"Re-creating locale $localeCode")
  dataService.deleteLocale(localeCode)
  dataService.createLocale(Locale(localeCode, englishLocaleName, localLocaleName, respondentLanguageCode, adminLanguageCode, flagCode, Some(baseLocaleCode)))

  // New local foods

  logger.info(s"Creating new main food records for $localeCode local foods (this will overwrite existing records!)")
  dataService.deleteFoods(newFoods.map(_.code)) // ignore recordnotfound errors
  throwOnError(dataService.createFoods(newFoods))

  // Recoded local foods

  val recodedLocalFoodRecords = buildRecodedLocalFoodRecords(options.logPath.get, englishLocaleName,
    localNutrientTableId, indexableFoods, recodingTable, associatedFoodTranslations)

  // Apply PSM from PSM tables and create local foods

  val localFoodRecordsWithoutPsm = newLocalFoodRecords ++ recodedLocalFoodRecords

  logger.info("Building PT to Intake food codes map")

  val localToIntakeCodeMap = localFoodRecordsWithoutPsm.foldLeft(Map[String, String]()) {
    case (result, (intakeCode, record)) =>
      record.nutrientTableCodes.get(localNutrientTableId) match {
        case Some(ptCode) => result + (ptCode -> intakeCode)
        case None => result
      }
  }

  logger.info(s"Loading $localeCode portion size method remapping table")
  val psmTable = psmTableParser.parsePortionSizeMethodsTable(options.psmTablePath(), options.localFoodTablePath(), localToIntakeCodeMap, indexableFoods, dataService)

  val localFoodRecords = localFoodRecordsWithoutPsm.foldLeft(Map[String, NewLocalFoodRecord]()) {
    case (result, (code, record)) => result + (code -> record.copy(portionSize = psmTable.getOrElse(code, Seq())))
  }

  logger.info(s"Creating local food records")
  throwOnError(dataService.createLocalFoodRecords(localFoodRecords, localeCode))

  // Category translations

  val newLocalCategoryRecords = indexableCategories.foldLeft(Map[String, NewLocalCategoryRecord]()) {
    (records, header) =>
      categoryTranslations.get(header.code) match {
        case Some(translation) =>
          records + (header.code -> NewLocalCategoryRecord(Some(translation), Seq()))
        case None => {
          logger.warn(s"Missing translation for category ${header.localDescription} (${header.code})")
          records
        }
      }
  }

  logger.info(s"Creating local category records")
  dataService.createLocalCategoryRecords(newLocalCategoryRecords, localeCode)

  logger.info(s"Done!")
}
