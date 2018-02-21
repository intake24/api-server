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

import scala.xml.{Node, NodeSeq}
import scala.xml.NodeSeq.seqToNodeSeq


object CategoryDef {

  def toXml(category: XmlCategoryRecord): Node =
    FoodDef.addPortionSizeMethods(
      FoodDef.addInheritableAttributes(
        <category code={category.code} description={category.description} hidden={category.isHidden.toString}>
          {category.foods.map(f => <food code={f}/>) ++
          category.subcategories.map(sc => <subcategory code={sc}/>)}
        </category>,
        category.attributes),
      category.portionSizeMethods)

  def toXml(categories: Seq[XmlCategoryRecord]): Node =
    <categories>
      {categories.map(toXml)}
    </categories>

  def parseXml(root: NodeSeq): Seq[XmlCategoryRecord] =
    (root \ "category").map(node => {
      val code = node.attribute("code").get.text
      val desc = node.attribute("description").get.text
      val hidden = node.attribute("hidden").get.text == "true"
      val foods = (node \ "food").map(_.attribute("code").get.text)
      val subcategories = (node \ "subcategory").map(_.attribute("code").get.text)

      val attr = FoodDef.inheritableAttributes(node)
      val psm = FoodDef.portionSizeMethods(node)

      XmlCategoryRecord(code, desc, foods, subcategories, hidden, attr, psm)
    })
}