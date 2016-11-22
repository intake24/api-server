package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import org.slf4j.LoggerFactory
import org.rogach.scallop.ScallopConf

import uk.ac.ncl.openlab.intake24.NutrientTable

import uk.ac.ncl.openlab.intake24.NutrientTableRecord

import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser
import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableMapping
import com.google.inject.Inject
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl

object PortugueseNutrientsImport extends App with WarningMessage with DatabaseConnection {

/*  val ptTableCode = "PT_INSA"
  val ptTableDescription = "Portuguese Food Composition Table (INSA)"
  
  import CsvNutrientTableParser.{ excelColumnToOffset => col, parseTable }  
  
  val csvIdColumnOffset = 0
  
  val csvRowOffset = 3
  
  def tableMapping(nutrient: Nutrient): Option[Int] = nutrient match {
    case Protein => Some(col("H"))
    case Fat => Some(col("I"))
    case Carbohydrate => Some(col("J"))
    case EnergyKcal => Some(col("E"))
    case EnergyKj => Some(col("F"))
    case Alcohol => Some(col("N"))
    case TotalSugars => Some(col("L"))
    case Nmes => None
    case SaturatedFattyAcids => Some(col("R"))
    case Cholesterol => Some(col("W"))
    case VitaminA => Some(col("Y"))
    case VitaminD => Some(col("AA"))
    case VitaminC => Some(col("AJ"))
    case VitaminE => Some(col("AB"))
    case Folate => Some(col("AK"))
    case Sodium => Some(col("AM"))
    case Calcium => Some(col("AO"))
    case Iron => Some(col("AR"))
    case DietaryFiber => Some(col("Q"))
    case TotalMonosaccharides => Some(col("K"))
    case OrganicAcids => Some(col("P"))
    case PolyunsaturatedFattyAcids => Some(col("T"))
    case NaCl => Some(col("X"))
    case Ash => Some(col("AL"))
  }
    
  val logger = LoggerFactory.getLogger(getClass)

  trait Options extends ScallopConf {
    version("Intake24 Portuguese food composition table import tool 16.7")

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  displayWarningMessage("WARNING: THIS WILL DESTROY ALL FOOD RECORDS HAVING PT FOOD COMPOSITION CODES!")

  val dataSource = getDataSource(options)

  val nutrientTableService = new FoodDatabaseAdminImpl(dataSource)

  nutrientTableService.deleteNutrientTable(ptTableCode)

  nutrientTableService.createNutrientTable(NutrientTable(ptTableCode, ptTableDescription))

  val table = CsvNutrientTableParser.parseTable(options.csvPath(), CsvNutrientTableMapping(csvRowOffset, csvIdColumnOffset, tableMapping))

  val records = table.records.map {
    case (code, nmap) =>
      NutrientTableRecord(ptTableCode, code, nmap)
  }.toSeq

  nutrientTableService.createNutrientTableRecords(records)*/

}
