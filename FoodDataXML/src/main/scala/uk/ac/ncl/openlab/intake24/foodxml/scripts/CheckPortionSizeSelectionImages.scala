/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.foodxml.scripts

import scala.xml.XML
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import java.io.File
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef

object CheckPortionSizeSelectionImages extends App {
  
  val imageDir = new File("/home/ivan/Projects/Intake24/intake24-images")
  
  val foods = FoodDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data-NEW ZEALAND/foods.xml"))
   
  foods.foreach {
    food =>
      if (food.localData.portionSize.size > 1) {
        food.localData.portionSize.foreach {
          ps =>
            val file = new File(imageDir.getAbsolutePath + File.separator + ps.imageUrl)
            if (!file.exists())
              println(s"Image ${ps.imageUrl} missing for ${food.code}") 
        }
      }
  }


}