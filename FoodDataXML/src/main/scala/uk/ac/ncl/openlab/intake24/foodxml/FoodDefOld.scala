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

import scala.xml.XML
import scala.xml.NodeSeq
import scala.xml.Node
import scala.xml.Attribute
import scala.xml.Text
import scala.xml.Null
import scala.xml.Elem
import net.scran24.fooddef.CategoryV1
import net.scran24.fooddef.FoodOld
import net.scran24.fooddef.PortionSizeMethod
import scala.xml.NodeSeq.seqToNodeSeq
import net.scran24.fooddef.PortionSizeMethodParameter

object FoodDefOld {
  def toXml(category: CategoryV1): Node =
    <category>
      {
        category.children.values.toSeq.sortBy(_.code).map {
          case f: FoodOld =>
            <food>
              {
                f.portionSize.map ( s =>
                   <portion-size>
                  		{ s.parameters.map( p => <param/> % Attribute (None, "value", Text(p.value), Null) 
                  									% Attribute (None, "name", Text(p.name), Null)) }
                  </portion-size>% Attribute (None, "method", Text(s.method), Null)
                )
              }
            </food> % 
            Attribute (None, "ndnsCode", Text(f.ndnsCode.toString), Null) %
            Attribute (None, "code", Text(f.code), Null) %
            Attribute (None, "isDrink", Text(if (f.isDrink) "true" else "false"), Null) %            
            Attribute (None, "description", Text(f.description), Null)

          case c: CategoryV1 => toXml(c)
        }
      }
    </category> % Attribute(None, "code", Text(category.code), Null) % Attribute(None, "description", Text(category.description), Null)

  def writeXml(path: String, root: Map[String, CategoryV1]) = {
      val doc = <scran24-foods>
	  	{
	  		root.values.toSeq.sortBy(_.code).map(toXml(_))
	  	}
	  </scran24-foods>
      
      XML.save(path, doc, "utf-8", true, null);
  }
    
  def parseParam (e: Elem) : PortionSizeMethodParameter = 
   PortionSizeMethodParameter(e.attribute("name").get.text, e.attribute("value").get.text)
    
  def parsePortionSize(e: Elem): PortionSizeMethod =
    if (e.label == "portion-size") PortionSizeMethod (e.attribute("method").get.text, e.attribute("description").map(_.text).getOrElse("No description"), 
        e.attribute("imageUrl").map(_.text).getOrElse("portion/placeholder.jpg"), e.attribute("useForRecipes").map(_.text.toBoolean).getOrElse(false), e.child.filter(_.isInstanceOf[Elem]).map(n => parseParam(n.asInstanceOf[Elem])))
    else throw new IllegalArgumentException ("Cannot parse element as portion size: " + e.text)

  def parseXml(root: NodeSeq): Map[String, CategoryV1] = {
    def rec(ns: NodeSeq, path: String): Map[String, CategoryV1] = {
      (ns \ "category").map(n => {

        val code = n.attribute("code").get.text
        val desc = n.attribute("description").get.text

        val curPath = if (path.isEmpty) code else path + ":" + code

        val foods = (n \ "food").map(fnode => {
          val code = fnode.attribute("code").get.text
          val desc = fnode.attribute("description").get.text
          val drink = fnode.attribute("isDrink").get.text == "true"
          val ndnsCode = fnode.attribute("ndnsCode").get.text.toInt
          
          val children = fnode.child.filter(_.isInstanceOf[Elem]).map(_.asInstanceOf[Elem])
          val portionSize = children.map(parsePortionSize(_))

          (code, FoodOld(code, desc, drink, ndnsCode, curPath, portionSize))
        }).toMap

        val subcats = rec(n, curPath)

        (code, CategoryV1(code, desc, subcats ++ foods, path))
      }).toMap
    }

    rec(root, "")
  } 
}
