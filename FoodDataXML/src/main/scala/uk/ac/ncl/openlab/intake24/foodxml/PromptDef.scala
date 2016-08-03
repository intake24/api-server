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
import uk.ac.ncl.openlab.intake24.AssociatedFoodV1
import scala.xml.NodeSeq.seqToNodeSeq
import uk.ac.ncl.openlab.intake24.AssociatedFoodV1

object PromptDef {
  def toXml(prompts: Map[String, Seq[AssociatedFoodV1]]) =
    prompts.keys.toSeq.sorted.map(foodCode =>
      <food>
        {
          prompts(foodCode).map(prompt =>
            <prompt/>
              % Attribute(None, "category", Text(prompt.category), Null)
              % Attribute(None, "text", Text(prompt.promptText), Null)
              % Attribute(None, "linkAsMain", Text(prompt.linkAsMain.toString), Null))
        }
      </food> % Attribute(None, "code", Text(foodCode), Null))

  def writeXml(path: String, prompts: Map[String, Seq[AssociatedFoodV1]]) = {
    val doc = <scran24-food-prompts>
                {
                  toXml(prompts)
                }
              </scran24-food-prompts>
    XML.save(path, doc, "utf-8", true, null);
  }

  def parseXml(root: NodeSeq): Map[String, Seq[AssociatedFoodV1]] = {
    (root \ "food").map(food => {
      val code = food.attribute("code").get.text
      
      val prompts = (food \ "prompt").map ( promptNode => AssociatedFoodV1(promptNode.attribute("category").get.text, promptNode.attribute("text").get.text, promptNode.attribute("linkAsMain").get.text == "true", promptNode.attribute("genericName").get.text))

      (code, prompts)
    }).toMap
  }
}