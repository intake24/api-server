package uk.ac.ncl.openlab.intake24.nutrientsndns

import uk.ac.ncl.openlab.intake24.nutrients._

object NzCsv {

  import CsvNutrientTableParser.{ excelColumnToOffset => col }

  val idColumnOffset = 0
  
  val rowOffset = 3

  def tableMapping(nutrientType: Nutrient): Option[Int] = nutrientType match {
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

}
