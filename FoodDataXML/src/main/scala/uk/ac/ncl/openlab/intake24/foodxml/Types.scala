package uk.ac.ncl.openlab.intake24.foodxml

import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.PortionSizeMethod

case class XmlFoodRecord(code: String, description: String, groupCode: Int, attributes: InheritableAttributes, nutrientTableCodes: Map[String, String],
    portionSizeMethods: Seq[PortionSizeMethod])
 
case class XmlCategoryRecord(code: String, description: String, foods: Seq[String], subcategories: Seq[String], isHidden: Boolean, attributes: InheritableAttributes, portionSizeMethods: Seq[PortionSizeMethod])

case class AssociatedFoodV1 (category: String, promptText: String, linkAsMain: Boolean, genericName: String)