package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter

import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

import com.google.inject.Inject
import com.google.inject.Singleton

import au.com.bytecode.opencsv.CSVWriter
import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.LocaleManagementSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.foodsql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.AssociatedFood


sealed trait FoodCodingDecision

case object DoNotUse extends FoodCodingDecision

case class UseUKFoodTable(localDescription: String) extends FoodCodingDecision

case class UseLocalFoodTable(localDescription: String, localTableRecordId: String) extends FoodCodingDecision

case class NewFoodRecord(englishDescription: String, localDescription: String, localTableRecordId: String)

case class RecodingTable(existingFoodsCoding: Map[String, FoodCodingDecision], newFoods: Seq[NewFoodRecord])

trait RecodingTableParser {
  def parseTable(path: String): RecodingTable
}

case class LocalFoodsImport(localeCode: String, englishLocaleName: String, localLocaleName: String,
    respondentLanguageCode: String, adminLanguageCode: String, flagCode: String, localNutrientTableId: String,
    recodingTableParser: RecodingTableParser) extends App with WarningMessage with DatabaseConnection {

  val logger = LoggerFactory.getLogger(getClass)

  val associatedFoodsParser = new AssociatedFoodTranslationParser()

  val categoryTranslationParser = new CategoryTranslationParser()

  val baseLocaleCode = "en_GB"

  trait Options extends ScallopConf {
    version("Intake24 food localisation data import tool 16.7")

    val recodingTablePath = opt[String](required = true, noshort = true)
    val categoryTranslationPath = opt[String](required = true, noshort = true)
    val associatedFoodsTranslationPath = opt[String](required = true, noshort = true)

    val logPath = opt[String](required = false, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage(s"WARNING: This will destroy all existing data for ${englishLocaleName} (${localeCode}) locale!")

  val dataSource = getDataSource(options)

  val dataService = new FoodDatabaseAdminImpl(dataSource)

  val indexDataService = new FoodIndexDataImpl(dataSource)

  // Should be an insert-update loop, but this is just a one-time script so using unsafe way
  val localeService = new LocaleManagementSqlImpl(dataSource)
  localeService.delete(localeCode)
  localeService.create(Locale(localeCode, englishLocaleName, localLocaleName, respondentLanguageCode, adminLanguageCode, flagCode, Some(baseLocaleCode)))

  val indexableFoods = indexDataService.indexableFoods(baseLocaleCode).right.get

  val logWriter = new CSVWriter(options.logPath.get.map(logPath => new FileWriter(new File(logPath))).getOrElse(new OutputStreamWriter(System.out)))

  logWriter.writeNext(Array("Intake24 code", "English food description", "Coding decision", s"$englishLocaleName description"))

  val recodingTable = recodingTableParser.parseTable(options.recodingTablePath())

  val associatedFoodTranslations = associatedFoodsParser.parseAssociatedFoodTranslation(options.associatedFoodsTranslationPath())

  indexableFoods.foreach {

    header =>

      logger.info(s"Processing food: ${header.localDescription} (${header.code})")

      val headerCols = Array(header.code, header.localDescription)

      dataService.foodRecord(header.code, localeCode) match {
        case Right(fooddef) => {
          val localData = fooddef.local

          def updateAssociatedFoods() = {
            associatedFoodTranslations.get(header.code) match {
              case Some(oldPrompts) => {
                logger.info(s"Updating associated foods for ${header.localDescription} (${header.code})")

                val prompts = oldPrompts.map {
                  v1 =>
                    val foodOrCategory = if (dataService.isCategoryCode(v1.category).right.get) Right(v1.category) else Left(v1.category)
                    AssociatedFood(foodOrCategory, v1.promptText, v1.linkAsMain, v1.genericName)
                }

                dataService.updateAssociatedFoods(header.code, localeCode, prompts)
              }
              case None => ()
              //logger.warn(s"No associated foods translations for ${header.localDescription} (${header.code})")
            }
          }

          try {
            recodingTable.existingFoodsCoding.get(header.code) match {
              case Some(DoNotUse) => {
                logWriter.writeNext(headerCols ++ Array(s"Not using in $englishLocaleName locale"))
                dataService.updateLocalFoodRecord(header.code, localeCode, localData.copy(doNotUse = true))
              }
              case Some(UseUKFoodTable(localDescription)) => {
                logWriter.writeNext(headerCols ++ Array("Inheriting UK food composition table code", localDescription))
                dataService.updateLocalFoodRecord(header.code, localeCode, localData.copy(localDescription = Some(localDescription), doNotUse = false))
                updateAssociatedFoods()
              }
              case Some(UseLocalFoodTable(localDescription, localTableRecordId)) => {
                logWriter.writeNext(headerCols ++ Array(s"Using $localNutrientTableId food composition table code", localDescription, localTableRecordId))
                dataService.updateLocalFoodRecord(header.code, localeCode, localData.copy(localDescription = Some(localDescription), nutrientTableCodes = Map(localNutrientTableId -> localTableRecordId), doNotUse = false))
                updateAssociatedFoods()
              }
              case None =>
                logWriter.writeNext(headerCols ++ Array(s"Not in $englishLocaleName recoding table!"))
            }

          } catch {
            case e: Throwable => throw new RuntimeException(s"Failed on ${header.code} (${header.localDescription})", e)
          }
        }
        case _ => throw new RuntimeException(s"Could not retrieve en_GB food definition for ${header.localDescription} (${header.code})")
      }
  }

  logWriter.close()

  val categoryTranslations = categoryTranslationParser.parseCategoryTranslations(options.categoryTranslationPath())

  val indexableCategories = indexDataService.indexableCategories(baseLocaleCode).right.get

  indexableCategories.foreach {
    header =>

      logger.info(s"Processing category: ${header.localDescription} (${header.code})")

      categoryTranslations.get(header.code) match {
        case Some(translation) => dataService.categoryRecord(header.code, localeCode) match {
          case Right(record) => {
            val localData = record.local
            dataService.updateCategoryLocal(header.code, localeCode, localData.copy(localDescription = Some(translation)))
          }
          case _ => throw new RuntimeException(s"Could not retrieve ${englishLocaleName} category record for ${header.localDescription} (${header.code})")
        }
        case None => logger.warn(s"Missing translation for category ${header.localDescription} (${header.code})")
      }
  }

}
