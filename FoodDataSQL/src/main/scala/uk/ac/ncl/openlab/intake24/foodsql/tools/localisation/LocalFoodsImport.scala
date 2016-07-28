package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import org.rogach.scallop.ScallopOption
import org.rogach.scallop.ScallopConf
import java.io.BufferedReader
import java.io.InputStreamReader
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import uk.ac.ncl.openlab.intake24.foodsql.AdminFoodDataServiceSqlImpl
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import uk.ac.ncl.openlab.intake24.services.CodeError
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import uk.ac.ncl.openlab.intake24.foodsql.IndexFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.LocaleManagementSqlImpl
import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.foodsql.NutrientDataManagementSqlImpl
import uk.ac.ncl.openlab.intake24.NutrientTable
import java.util.Properties
import java.io.PrintWriter
import com.zaxxer.hikari.HikariConfig
import uk.ac.ncl.openlab.intake24.foodsql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseOptions
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import au.com.bytecode.opencsv.CSVWriter

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
  
  val baseLocaleCode = "en_GB"

  trait Options extends ScallopConf {
    version("Intake24 food localisation data import tool 16.7")

    val recodingTablePath = opt[String](required = true, noshort = true)
    val logPath = opt[String](required = false, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage(s"WARNING: This will destroy all existing data for ${englishLocaleName} (${localeCode}) locale!")

  val dataSource = getDataSource(options)

  val dataService = new AdminFoodDataServiceSqlImpl(dataSource)

  val indexDataService = new IndexFoodDataServiceSqlImpl(dataSource)

  // Should be an insert-update loop, but this is just a one-time script so using unsafe way
  val localeService = new LocaleManagementSqlImpl(dataSource)
  localeService.delete(localeCode)
  localeService.create(Locale(localeCode, englishLocaleName, localLocaleName, respondentLanguageCode, adminLanguageCode, flagCode, Some(baseLocaleCode)))

  val indexableFoods = indexDataService.indexableFoods(baseLocaleCode)

  val logWriter = new CSVWriter(options.logPath.get.map(logPath => new FileWriter(new File(logPath))).getOrElse(new OutputStreamWriter(System.out)))

  logWriter.writeNext(Array("Intake24 code", "English food description", "Coding decision", s"$englishLocaleName description"))

  val recodingTable = recodingTableParser.parseTable(options.recodingTablePath())

  indexableFoods.foreach {
    header =>

      val headerCols = Array(header.code, header.localDescription)

      dataService.foodRecord(header.code, localeCode) match {
        case Right(fooddef) => {
          val localData = fooddef.local

          try {
          recodingTable.existingFoodsCoding.get(header.code) match {
            case Some(DoNotUse) => {
              logWriter.writeNext(headerCols ++ Array(s"Not using in $englishLocaleName locale"))
              dataService.updateFoodLocal(header.code, localeCode, localData.copy(doNotUse = true))
            }
            case Some(UseUKFoodTable(localDescription)) => {
              logWriter.writeNext(headerCols ++ Array("Inheriting UK food composition table code", localDescription))
              dataService.updateFoodLocal(header.code, localeCode, localData.copy(localDescription = Some(localDescription), doNotUse = false))
            }
            case Some(UseLocalFoodTable(localDescription, localTableRecordId)) => {
              logWriter.writeNext(headerCols ++ Array(s"Using $localNutrientTableId food composition table code", localDescription, localTableRecordId))
              dataService.updateFoodLocal(header.code, localeCode, localData.copy(localDescription = Some(localDescription), nutrientTableCodes = Map(localNutrientTableId -> localTableRecordId), doNotUse = false))
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

  /*categories.foreach {
    case (code, translation) =>
      dataService.categoryRecord(code, danishLocaleCode) match {
        case Right(record) => {
          val localData = record.local
          dataService.updateCategoryLocal(code, danishLocaleCode, localData.copy(localDescription = Some(translation)))
        }
        case _ => throw new RuntimeException(s"Could not retrieve category record for ${code}")
      }
  }*/
  
  logWriter.close()
}
