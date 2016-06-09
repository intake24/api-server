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

package uk.ac.ncl.openlab.intake24.admin

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.Array
import org.scalajs.jquery._
import org.scalajs.dom._
import org.scalajs.dom.ext._
import org.scalajs.dom.html._
import scalatags.JsDom.all._
import upickle.default._
import scala.util.Failure
import scala.util.Success
import net.scran24.fooddef.Category
import scala.scalajs.js.JSON
import js.JSConverters._
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.FoodHeader
import uk.ac.ncl.openlab.intake24.api.Intake24Credentials
import scala.concurrent.ExecutionContext.Implicits.global
import uk.ac.ncl.openlab.viands.dbtool.ui.FoodBrowser
import uk.ac.ncl.openlab.viands.dbtool.jstree.JsTreeNode
import uk.ac.ncl.openlab.viands.dbtool.ui.FoodDef
import upickle.Invalid
import uk.ac.ncl.openlab.intake24.admin.views.SBAdmin

object Admin extends JSApp {

  def main = {
    
    val nav = SBAdmin.navigation
    
    document.body.appendChild(nav.render)
    
    
/*
    val client = ApiClient(JsGlobals.viandsApiHost)

    val treeDiv = document.createElement("div")
    treeDiv.setAttribute("id", "test")

    val foodDiv = document.createElement("div")
    foodDiv.setAttribute("id", "foodDiv")

    document.body.appendChild(treeDiv)
    document.body.appendChild(foodDiv)

    def foodBrowserNodeSelected(node: JsTreeNode) = node.li_attr(FoodBrowser.nodeTypeAttr) match {
      case FoodBrowser.nodeTypeFood => {
        val code = node.id.substring(FoodBrowser.nodePrefix.length())
        println(s"Selected $code")

        val f = for {
          foodData <- client.foodDef(code)
          foodGroups <- client.allFoodGroups()
        } yield {
          (foodData, foodGroups)
        }

        f.onComplete {
          case Success((foodData, foodGroups)) => foodDiv.appendChild(FoodDef(foodData, foodGroups).elem)
          case Failure(e) => println(e.getMessage)
        }
      }
      case FoodBrowser.nodeTypeCategory => window.alert("Category!")
    }

    client.authenticate(Intake24Credentials("", "admin", "intake24")).onComplete {
      case Failure(e) => {
        println("Failure")
        println(e.getMessage)
      }
      case Success(AuthenticationResult.Success) => {
        println("Success")

        val q = jQuery("#test")

        val foodBrowser = FoodBrowser(client, q, foodBrowserNodeSelected)

      }
    }*/
  }
}