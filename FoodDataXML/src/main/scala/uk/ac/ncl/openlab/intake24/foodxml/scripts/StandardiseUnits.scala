package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import scala.xml.XML
import net.scran24.fooddef.PortionSizeMethodParameter

object StandardiseUnits extends App {
    val dataDir = "/home/ivan/Projects/Intake24/intake24-data"
    
    val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))
    
    def parseUnits(params: Seq[PortionSizeMethodParameter]): Seq[String] = {
      val count = params.find(_.name == "units-count").get.value.toInt
      
      Range(0, count).map {
        index => params.find(_.name == s"unit$index-name").get.value        
      }
    }
      
    
    val units = foods.flatMap {
      food =>
        food.localData.portionSize.filter(_.method == "standard-portion").flatMap(x => parseUnits(x.parameters))
    }
    
    
    println ("Total number: " + units.length)
    
    val distinct = units.distinct
    
    println ("Distinct number: " + distinct.length)
    
    distinct.sorted.foreach(println)
}