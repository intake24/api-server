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

import scala.xml._
import scala.xml.NodeSeq.seqToNodeSeq

object BrandDef {
  def toXml(brands: Map[String, Seq[String]]) =
    <brand-names>
      {
    		brands.keySet.toSeq.sorted.map ( code => {
    		  <food code = {code}>
    		  {
    		    brands(code).sorted.map ( name => {
    		      <brand name = {name}/>
    		    })    		  
    		  }
    		  </food>
    		})
      }
    </brand-names>

  def parseXml(root: NodeSeq) = {
    (root \ "food").foldLeft(Map[String, Seq[String]]())( (map, elem) => {
      val code = elem.attribute("code").get.text
      val names = (elem \ "brand").map ( bnode => bnode.attribute("name").get.text)
      map + (code -> names)
    })
  }
}