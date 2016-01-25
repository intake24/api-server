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

package uk.ac.ncl.openlab.viands.dbtool.ui

import scala.scalajs.js
import uk.ac.ncl.openlab.viands.dbtool.ViandsClient
import org.scalajs.jquery.JQuery
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.FoodHeader
import uk.ac.ncl.openlab.viands.dbtool.jstree.JsTreeNode
import uk.ac.ncl.openlab.viands.dbtool.jstree.JsTree
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import org.scalajs.jquery.JQueryEventObject
import uk.ac.ncl.openlab.viands.dbtool.jstree.JsTreeSelectEvent
import scala.ref.WeakReference

case class FoodBrowser(client: ViandsClient, element: JQuery, onNodeSelected: JsTreeNode => Unit) {

  import FoodBrowser._
  
  val tree = JsTree.createTree(element, loadChildNodes)

  element.on("select_node.jstree", (event: JQueryEventObject, data: JsTreeSelectEvent) => {
    onNodeSelected(data.node)
  })
      
  def createNode(header: CategoryHeader): JsTreeNode = JsTree.createNode(s"$nodePrefix${header.code}", header.englishDescription, true, Map(nodeTypeAttr -> nodeTypeCategory))

  def createNode(header: FoodHeader): JsTreeNode = JsTree.createNode(s"food_browser_node_${header.code}", header.englishDescription, false, Map(nodeTypeAttr -> nodeTypeFood))

  def loadChildNodes(parentNode: JsTreeNode): Future[Seq[JsTreeNode]] = parentNode.id match {
    case "#" => client.rootCategories.map(_.map(createNode))

    case code => client.categoryContents(code.substring(nodePrefix.length())).map {
      categoryContents =>
        val subcategories = categoryContents.subcategories.map(createNode)
        val foods = categoryContents.foods.map(createNode)

        subcategories ++ foods
    }
  }
}

object FoodBrowser {
  val nodePrefix = "food_browser_node_"
  val nodeTypeAttr = "data-node-type"
  val nodeTypeCategory = "category"
  val nodeTypeFood = "food"
}