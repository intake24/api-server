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
import net.scran24.fooddef.PortionSizeMethod
import net.scran24.fooddef.PortionSizeMethod
import net.scran24.fooddef.InheritableAttributes
import scala.sys.process._
import net.scran24.fooddef.DrinkScale
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.GuideImageDef
import uk.ac.ncl.openlab.intake24.foodxml.DrinkwareDef
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import uk.ac.ncl.openlab.intake24.foodxml.Util
import au.com.bytecode.opencsv.CSVWriter
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import collection.JavaConversions._

object PortionSizeImagesForRecoding extends App {

  val foodsPath = "/home/ivan/Projects/Intake24/intake24-data/foods.xml"

  val asServedPath = "/home/ivan/Projects/Intake24/intake24-data/as-served.xml"
  val guidePath = "/home/ivan/Projects/Intake24/intake24-data/guide.xml"
  val drinkScalePath = "/home/ivan/Projects/Intake24/intake24-data/drinkware.xml"

  val codeOrderSrc = "/home/ivan/tmp/port.csv"

  val imageBase = "https://intake24.co.uk/intake24-images/"

  val foods = FoodDef.parseXml(XML.load(foodsPath))
  val asServed = AsServedDef.parseXml(XML.load(asServedPath))
  val guide = GuideImageDef.parseXml(XML.load(guidePath))
  val drinkware = DrinkwareDef.parseXml(XML.load(drinkScalePath))

  def description(method: PortionSizeMethod): String = {
    method.method match {
      case "as-served" => "As served"

      case "guide-image" => "Guide image"

      case "drink-scale" => "Sliding scale"

      case "standard-portion" => "Standard portion"

      case "cereal" => "Cereal"

      case "milk-in-a-hot-drink" => "Milk in a hot drink"

      case "milk-on-cereal" => "Milk on cereal"

      case "pizza" => "Pizza"

      case _ => throw new RuntimeException("Unexpected portion size method name: " + method.method)
    }
  }

  def generateImageLink(method: PortionSizeMethod): String = {

    val params = method.parameters.map(p => (p.name, p.value)).toMap

    method.method match {
      case "as-served" => {
        val set = asServed(params("serving-image-set"))
        val imageIndex = set.images.length / 2

        imageBase + set.images(imageIndex).url
      }

      case "guide-image" => {
        val guideImage = guide(params("guide-image-id"))

        imageBase + guideImage.id + ".jpg"
      }

      case "drink-scale" => {
        val drinkScale = drinkware(params("drinkware-id"))

        imageBase + drinkScale.guide_id + ".jpg"
      }

      case "standard-portion" => "No image (standard portion)"

      case "cereal" => imageBase + "cereal/flakeA4.jpg"

      case "milk-in-a-hot-drink" => "No image (milk in a hot drink)"

      case "milk-on-cereal" => imageBase + "cereal/milkbowlA.jpg"

      case "pizza" => imageBase + "gpiz2.jpg"

      case _ => throw new RuntimeException("Unexpected portion size method name: " + method.method)
    }
  }

  val reader = new CSVReader(new FileReader(codeOrderSrc))

  val codes = reader.readAll().toSeq.map(_(0))

  val writer = new CSVWriter(new FileWriter("/home/ivan/tmp/psimages.csv"))

  writer.writeNext(Array("Intake24 food code", "Intake24 description", "Portion size images"))

  val foodMap = foods.map(f => (f.code, f)).toMap

  codes.foreach {
    code =>
      foodMap.get(code) match {
        case Some(food) => {
          val header = Array(food.code, food.englishDescription)

          val psmImages = food.localData.portionSize.map {
            psm => s"${generateImageLink(psm)}"
          }.toArray

          writer.writeNext(header ++ psmImages)
        }
        case None => {
          println("Missing record for " + code)
          writer.writeNext(Array())
        }
      }
  }

  writer.close
}