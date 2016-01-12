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

package uk.ac.ncl.openlab.viands.dbtool

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.Array
import org.scalajs.jquery._
import org.scalajs.dom._
import org.scalajs.dom.ext._
import org.scalajs.dom.html._
import scalatags.JsDom.all._
import upickle.default._
import uk.ac.ncl.openlab.intake24.api.Intake24Credentials
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import net.scran24.fooddef.Category
import scala.scalajs.js.JSON
import js.JSConverters._
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.FoodHeader

trait JsTreeInstance extends js.Object {
  def create_node(par: String, node: js.Object, pos: String): Any = js.native
}

trait JsTreeNodeState extends js.Object {
  val loaded: Boolean = js.native
  val failed: Boolean = js.native
  val loading: Boolean = js.native
}

trait JsTreeNode extends js.Object {
  val id: String = js.native
  val parent: String = js.native
  val parents: Seq[js.Any] = js.native
  val children: js.Any = js.native
  val children_d: Seq[js.Any] = js.native
  val state: JsTreeNodeState = js.native
}

trait JsTreeStatic extends js.Object {
  def create(el: js.Any, options: js.Object): JsTreeInstance = js.native
}

trait JQueryStaticWithJsTree extends JQueryStatic {
  val jstree: JsTreeStatic = js.native
}

object JQueryStaticWithJsTree {
  implicit def addjstree(jq: JQueryStatic): JQueryStaticWithJsTree = jq.asInstanceOf[JQueryStaticWithJsTree]
}

object DBTool extends JSApp {
  import JQueryStaticWithJsTree._

  def mkNode(id: String, text: String, hasChildren: Boolean): JsTreeNode = {
    js.Dynamic.literal(id = id, text = text, children = hasChildren).asInstanceOf[JsTreeNode]
  }
  
  def mkNode(header: CategoryHeader): JsTreeNode = mkNode(header.code, header.description, true)
  
  def mkNode(header: FoodHeader): JsTreeNode = mkNode(header.code, header.description, false)

  val loadNode = (tree: JsTreeInstance, node: JsTreeNode, callback: js.Function1[js.Array[JsTreeNode], _]) => {
    node.id match {
      case "#" => {
        ViandsClient.rootCategories.onComplete {
          case Success(categories) =>
            callback.apply(categories.map(mkNode).toJSArray)
          case Failure(e: Throwable) => window.alert(e.toString())
        }
      }
      case code => {
        ViandsClient.categoryContents(code).onComplete { 
          case Success(categoryContents) => {
            val subcategories = categoryContents.subcategories.map(mkNode)
            val foods = categoryContents.foods.map(mkNode)
            
            callback.apply((subcategories ++ foods).toJSArray)
          }
         }        
      }
    }
  }

  def main = {

    val treeDiv = document.createElement("div")
    treeDiv.setAttribute("id", "test")

    document.body.appendChild(treeDiv)

    println("root created")

    ViandsClient.authenticate(Intake24Credentials("", "admin", "intake24")).onComplete {
      case Failure(e) => println(e.getMessage)
      case Success(_) => {
        val tree = jQuery.jstree.create("#test", js.Dynamic.literal(core = js.Dynamic.literal(check_callback = true, data = loadNode: js.ThisFunction)))
      }
    }
  }
}