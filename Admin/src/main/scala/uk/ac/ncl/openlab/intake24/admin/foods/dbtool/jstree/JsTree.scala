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

package uk.ac.ncl.openlab.viands.dbtool.jstree

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import org.scalajs.jquery._
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Dictionary

@js.native
trait JsTreeInstance extends js.Object {
  def create_node(par: String, node: js.Object, pos: String): Any = js.native    
}

@js.native
trait JsTreeNodeState extends js.Object {
  val loaded: Boolean = js.native
  val failed: Boolean = js.native
  val loading: Boolean = js.native
}

@js.native
trait JsTreeNode extends js.Object {
  val id: String = js.native
  val parent: String = js.native
  val parents: Seq[js.Any] = js.native
  val children: js.Any = js.native
  val children_d: Seq[js.Any] = js.native
  val state: JsTreeNodeState = js.native
  val li_attr: Dictionary[String] = js.native
} 

@js.native 
trait JsTreeSelectEvent extends js.Object {
  val node: JsTreeNode = js.native
  val selected: Array[String] = js.native
  val event: js.Any = js.native
}

@js.native
trait JsTreeStatic extends js.Object {
  def create(el: js.Any, options: js.Object): JsTreeInstance = js.native
}
@js.native
trait JQueryStaticWithJsTree extends JQueryStatic {
  val jstree: JsTreeStatic = js.native
}

object JQueryStaticWithJsTree {
  implicit def addjstree(jq: JQueryStatic): JQueryStaticWithJsTree = jq.asInstanceOf[JQueryStaticWithJsTree]
}

object JsTree {
  
  import JQueryStaticWithJsTree._
  
  def createNode(id: String, text: String, hasChildren: Boolean, li_attr: Map[String, String]): JsTreeNode = {
    js.Dynamic.literal(id = id, text = text, children = hasChildren, li_attr = li_attr.toJSDictionary).asInstanceOf[JsTreeNode]
  }
  
  def createTree(element: JQuery, loadNodes: JsTreeNode => Future[Seq[JsTreeNode]]): JsTreeInstance = {
    val jsLoadNode = (tree: JsTreeInstance, node: JsTreeNode, callback: js.Function1[js.Array[JsTreeNode], _]) => {
      loadNodes(node).onComplete { 
        case Success(nodes) => callback.apply(nodes.toJSArray)
        case Failure(exception) => exception.printStackTrace()
      }
       
    }
    jQuery.jstree.create(element, js.Dynamic.literal(core = js.Dynamic.literal(check_callback = true, data = jsLoadNode: js.ThisFunction)))        
  }    
  
}