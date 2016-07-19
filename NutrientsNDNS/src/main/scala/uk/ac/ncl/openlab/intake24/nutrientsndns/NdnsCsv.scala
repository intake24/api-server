package uk.ac.ncl.openlab.intake24.nutrientsndns

import uk.ac.ncl.openlab.intake24.nutrients._

object NdnsCsv {

  val idColumnOffset = 0
  
  val rowOffset = 1

  def tableMapping(nutrientType: Nutrient): Option[Int] = nutrientType match {
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
}
