package uk.ac.ncl.openlab.intake24.nutrientsndns

import uk.ac.ncl.openlab.intake24.nutrients._

object LegacyNutrientTables {
  
  import CsvNutrientTableParser.{ excelColumnToOffset => col }

  private def ndnsCsvNutrientMapping(nutrientType: Nutrient): Option[Int] = nutrientType match {
    case Protein => Some(20)
    case Fat => Some(22)
    case Carbohydrate => Some(24)
    case EnergyKcal => Some(26)
    case EnergyKj => Some(28)
    case Alcohol => Some(30)
    case TotalSugars => Some(38)
    case Nmes => Some(40)
    case SaturatedFattyAcids => Some(56)
    case Cholesterol => Some(66)
    case VitaminA => Some(78)
    case VitaminD => Some(80)
    case VitaminC => Some(92)
    case VitaminE => Some(94)
    case Folate => Some(100)
    case Sodium => Some(106)
    case Calcium => Some(110)
    case Iron => Some(116)
    case Zinc => Some(124)
    case Selenium => Some(132)
    case DietaryFiber => None
    case TotalMonosaccharides => None
    case OrganicAcids => None
    case PolyunsaturatedFattyAcids => None
    case NaCl => None
    case Ash => None
  }

  val ndnsCsvTableMapping = CsvNutrientTableMapping(1, 0, ndnsCsvNutrientMapping)

  private def nzCsvNutrientMapping(nutrientType: Nutrient): Option[Int] = nutrientType match {
    case Protein => Some(col("BP"))
    case Fat => Some(col("AF"))
    case Carbohydrate => Some(col("I"))
    case EnergyKcal => Some(col("X"))
    case EnergyKj => Some(col("Z"))
    case Alcohol => Some(col("D"))
    case TotalSugars => Some(col("BW"))
    case Nmes => None
    case SaturatedFattyAcids => Some(col("AS"))
    case Cholesterol => Some(col("R"))
    case VitaminA => Some(col("CB"))
    case VitaminD => Some(col("CF"))
    case VitaminC => Some(col("CE"))
    case VitaminE => Some(col("CG"))
    case Folate => Some(col("AY"))
    case Sodium => Some(col("BT"))
    case Calcium => Some(col("P"))
    case Iron => Some(col("BE"))
    case Zinc => Some(col("CI"))
    case Selenium => Some(col("BS"))
    case DietaryFiber => None
    case TotalMonosaccharides => None
    case OrganicAcids => None
    case PolyunsaturatedFattyAcids => None
    case NaCl => None
    case Ash => None
  }

  val nzCsvTableMapping = CsvNutrientTableMapping(3, 0, nzCsvNutrientMapping)
}