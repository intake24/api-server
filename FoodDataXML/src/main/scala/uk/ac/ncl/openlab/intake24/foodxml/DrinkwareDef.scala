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

import scala.xml.Attribute
import scala.xml.Text
import scala.xml.Null
import scala.xml.XML
import scala.xml.NodeSeq
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.VolumeFunction
import scala.xml.NodeSeq.seqToNodeSeq

import uk.ac.ncl.openlab.intake24.DrinkScale

object DrinkwareDef {
  def toXml(sets: Iterable[DrinkwareSet]) =
    sets.toSeq.sortBy(_.id).map(set =>
      <drinkware>
        <choice/>
        % Attribute (None, "guide-id", Text(set.guide_id), Null)
        {
          set.scaleDefs.sortBy(_.choice_id).map(scale => {
            <scale>
              <dimensions/> % 
        	  	Attribute (None, "width", Text(scale.width), Null) %
    	    	Attribute (None, "height", Text(scale.width), Null) %
    	    	Attribute (None, "emptyLevel", Text(scale.width), Null) %
        	  	Attribute (None, "fullLevel", Text(scale.width), Null)
        	  <volume-function>
        	     { scale.vf.samples.map ( s => <value/> % Attribute (None, "fill", Text(s._1.toString), Null) 
        	    		 								% Attribute (None, "volume", Text(s._2.toString), Null)) }
        	  </volume-function>
        	  <baseImage>{ scale.baseImage }</baseImage>
        	  <overlayImage>{ scale.overlayImage }</overlayImage>
            </scale> % Attribute(None, "choice-id", Text(scale.choice_id.toString), Null)
          })
        }
      </drinkware> % Attribute(None, "id", Text(set.id), Null) % Attribute(None, "description", Text(set.description), Null))

  def writeXml(path: String, sets: Iterable[DrinkwareSet]) = {
    val doc = <scran24-drinks-scale>
                {
                  toXml(sets)
                }
              </scran24-drinks-scale>

    XML.save(path, doc, "utf-8", true, null);
  }
  
  def parseVolumeFunc (n: NodeSeq): VolumeFunction = 
    VolumeFunction((n \ "value").map ( n => {
      (n.attribute("fill").get.text.toDouble, n.attribute("volume").get.text.toDouble)
     }))

  def parseXml(root: NodeSeq) = {
    (root \ "drinkware").map(set => {
      val choice = (set \ "choice").head.attribute("guide-id").get.text
      val scales = (set \ "scale").map(n => {
        val choice_id = n.attribute ("choice-id").get.text.toInt
        val dimNode = (n \ "dimensions").head
        val width = dimNode.attribute("width").get.text.toInt
        val height = dimNode.attribute("height").get.text.toInt
        val emptyLevel = dimNode.attribute("emptyLevel").get.text.toInt
        val fullLevel = dimNode.attribute("fullLevel").get.text.toInt
        val baseImage = (n \ "baseImage").head.text
        val overlayImage = (n \ "overlayImage").head.text
        val vf = parseVolumeFunc ((n \ "volume-function").head)
        
        DrinkScale (choice_id, baseImage, overlayImage, width, height, emptyLevel, fullLevel, vf)
      })
      
      val id = set.attribute("id").get.text
      val description = set.attribute("description").get.text

      (id, DrinkwareSet(id, description, choice, scales))
    }).toMap
  }
}