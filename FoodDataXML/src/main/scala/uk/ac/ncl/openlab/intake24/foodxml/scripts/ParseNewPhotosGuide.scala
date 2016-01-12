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

import java.io.FileReader
import scala.collection.JavaConversions.asScalaBuffer
import au.com.bytecode.opencsv.CSVReader

import net.scran24.fooddef.AsServedImage
import net.scran24.fooddef.AsServedSet
import scala.xml.XML
import java.io.File
import java.io.FileFilter
import net.scran24.fooddef.PortionSizeMethod
import net.scran24.fooddef.GuideImageWeightRecord
import net.scran24.fooddef.GuideImage

object ParseNewPhotosGuide {
 /* import ParseNewPhotos._

  def main(args: Array[String]): Unit = {
    val sourcePath = "D:\\SCRAN24\\Notes\\new_guides.csv"
    val foodsSrc = "D:\\SCRAN24\\Data\\foods.xml"
    val foodsDst = "D:\\SCRAN24\\Data\\foods-newguide.xml"
    val guideSrc = "D:\\SCRAN24\\Data\\guide.xml"
    val guideDst = "D:\\SCRAN24\\Data\\guide-new.xml"
    val photosBase = "D:\\SCRAN24\\Photos_new"

    //	FOOD (for reference)	PHOTO TYPE	WEIGHT (g)	photo code	food code(s) linked to																		
    val rows = new CSVReader(new FileReader(sourcePath)).readAll().toSeq.map(_.toSeq)

    val parsedRows = rows.tail.map(r => Row(r(0), r(1), r(2), r(3), r.drop(4).filterNot(_.isEmpty).filter(_.length == 4)))

    val grouped = parsedRows.groupBy(r => r.foods)

    val oldGuideImages = GuideImageDef.parseXml(XML.load(guideSrc))

    val newGuideImages = grouped.values.toSeq.map(rows => {
      val id = mkid(rows.map(_.id))
      println(id)
      rows.foreach(println)
      val weights = rows.map(row => GuideImageWeightRecord(row.description, row.id.substring(id.length).toInt, row.weight.toDouble))
      (id, GuideImage(id, "No description", weights))
    }).toMap

    Util.writeXml(GuideImageDef.toXml((oldGuideImages ++ newGuideImages).values.toSeq.sortBy(_.id)), guideDst)

    val newGuideImageAssoc = grouped.mapValues(rows => mkid(rows.map(_.id))).toSeq

    val foods = FoodDef.parseXml(XML.load(foodsSrc)).map(f => (f.code, f)).toMap

    val updatedFoods = newGuideImageAssoc.foldLeft(foods) {
      case (foods, (assocFoods, guideId)) => assocFoods.foldLeft(foods) {
        case (foods, code) => foods.get(code) match {
          case Some(food) => {
            println("Updating " + foods(code))
            foods + (code -> foods(code).copy(portionSize = foods(code).portionSize :+ PortionSizeMethod("guide-image", "No description", "portion/placeholder.jpg", Seq(("guide-image-id", guideId)))))
          }
          case None => {
            println("Missing food: " + code)
            foods
          }
        }
      }
    }

    Util.writeXml(FoodDef.toXml(updatedFoods.values.toSeq.sortBy(_.description)), foodsDst)
  }*/
}