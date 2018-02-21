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

import uk.ac.ncl.openlab.intake24.api.data.{GuideImage, GuideImageWeightRecord}

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq

object GuideImageDef {
  def toXml(guideImages: Seq[GuideImage]) =
    <guide-images>
      {
        guideImages.map(image =>
          <guide-image id={ image.id } description={ image.description }>
            {
              image.weights.map(weight =>
                <weight id={ weight.objectId.toString() } description={ weight.description } value={ weight.weight.toString() }/>)
            }
          </guide-image>)

      }
    </guide-images>

  def parseXml(root: NodeSeq): Map[String, GuideImage] = {
    (root \ "guide-image").map(n => {
      val name = n.attribute("id").get.text
      val desc = n.attribute("description").map(_.text).getOrElse("(no description for guide image " + name + ")");

      val weights = (n \ "weight").map(n => {
        val desc = n.attribute("description").get.text
        val id: java.lang.Integer = n.attribute("id").get.text.toInt
        val value: java.lang.Double = n.attribute("value").get.text.toDouble

        GuideImageWeightRecord(desc, id, value)
      })

      (name, GuideImage(name, desc, weights))
    }).toMap
  }
}