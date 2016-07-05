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

package uk.ac.ncl.openlab.intake24.foodxml

import scala.xml.NodeSeq
import scala.xml.Node
import scala.xml.Attribute
import scala.xml.Text
import scala.xml.Null
import scala.xml.Elem

import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import scala.xml.NodeSeq.seqToNodeSeq
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter

import java.util.UUID
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord

object FoodDef {
  
  val log = LoggerFactory.getLogger(getClass())

  def toXml(portionSizeMethod: PortionSizeMethod) =
    <portion-size method={ portionSizeMethod.method } description={ portionSizeMethod.description } imageUrl={ portionSizeMethod.imageUrl } useForRecipes={ portionSizeMethod.useForRecipes.toString }>
      {
        portionSizeMethod.parameters.map(p => <param name={ p.name } value={ p.value }/>)
      }
    </portion-size>

  def toXml(nutrientTables: Map[String, String]) =
    nutrientTables.keys.map {
      key =>
        <nutrient-table id={ key } code={ nutrientTables(key) }/>
    }

  def toXml(food: FoodRecord): Node =
    addInheritableAttributes(
      <food code={ food.main.code } description={ food.main.englishDescription } groupCode={ food.main.groupCode.toString }>
        { toXml(food.local.nutrientTableCodes) }
        { food.local.portionSize.map(toXml) }
      </food>,
      food.main.attributes)

  def toXml(foods: Seq[FoodRecord]): Node =
    <foods>
      { foods.map(toXml) }
    </foods>

  def parseParam(e: Elem): PortionSizeMethodParameter =
    PortionSizeMethodParameter(e.attribute("name").get.text, e.attribute("value").get.text)

  def parsePortionSize(e: Elem): PortionSizeMethod =
    if (e.label == "portion-size") PortionSizeMethod(e.attribute("method").get.text, e.attribute("description").map(_.text).getOrElse("No description"),
      e.attribute("imageUrl").map(_.text).getOrElse("portion/placeholder.jpg"), e.attribute("useForRecipes").map(_.text.toBoolean).getOrElse(false), e.child.filter(_.isInstanceOf[Elem]).map(n => parseParam(n.asInstanceOf[Elem])))
    else throw new IllegalArgumentException("Cannot parse element as portion size: " + e.label)

  def inheritableAttributes(e: Node): InheritableAttributes = {
    val readyMealOption = e.attribute("readyMealOption").map(_.text == "true")
    val sameAsBeforeOption = e.attribute("sameAsBeforeOption").map(_.text == "true")
    val reasonableAmount = e.attribute("reasonableAmount").map(_.text.toInt)

    InheritableAttributes(readyMealOption, sameAsBeforeOption, reasonableAmount)
  }

  def portionSizeMethods(e: Node): Seq[PortionSizeMethod] =
    (e \ "portion-size").map(n => parsePortionSize(n.asInstanceOf[Elem]))

  def addPortionSizeMethods(e: Elem, psm: Seq[PortionSizeMethod]) =
    e.copy(child = e.child ++ psm.map(toXml))

  def addInheritableAttributes(e: Elem, attr: InheritableAttributes) = {
    var result = e

    attr.readyMealOption.foreach(readyMealOption => result %= Attribute(None, "readyMealOption", Text(readyMealOption.toString), Null))
    attr.sameAsBeforeOption.foreach(sameAsBeforeOption => result %= Attribute(None, "sameAsBeforeOption", Text(sameAsBeforeOption.toString), Null))
    attr.reasonableAmount.foreach(reasonableAmount => result %= Attribute(None, "reasonableAmount", Text(reasonableAmount.toString), Null))

    result
  }

  def parseNutrientTableCodes(e: Node): Map[String, String] =
    (e \ "nutrient-table").map {
      n =>
        (n.attribute("id").get.text, n.attribute("code").get.text)
    }.toMap

  def parseXml(root: NodeSeq): Seq[FoodRecord] =
    (root \ "food").map(fnode => {
      val code = fnode.attribute("code").get.text
      val desc = fnode.attribute("description").get.text
      val groupCode = fnode.attribute("groupCode").map(_.text.toInt).getOrElse(0)

      val attribs = inheritableAttributes(fnode)

      val psm = portionSizeMethods(fnode)
      
      val legacyNdns = fnode.attribute("ndnsCode").map(_.text.toInt)
      
      val nutrientTables = legacyNdns match {
        case Some(code) if code != -1 => parseNutrientTableCodes(fnode) + ("NDNS" -> code.toString())
        case _ => parseNutrientTableCodes(fnode)
      }

      FoodRecord(
          MainFoodRecord(UUID.randomUUID(), code, desc, groupCode, attribs), 
          LocalFoodRecord(Some(UUID.randomUUID()), Some(desc), false, nutrientTables , psm))
    })
}