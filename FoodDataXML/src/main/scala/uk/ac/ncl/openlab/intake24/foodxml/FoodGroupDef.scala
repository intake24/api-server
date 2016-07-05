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
import uk.ac.ncl.openlab.intake24.FoodGroup
import scala.xml.NodeSeq.seqToNodeSeq

object FoodGroupDef {
  def toXml(groups: Seq[FoodGroup]) =
    <food-groups>
      {
        groups.sortBy(_.id).map(group =>
          <group id={ group.id.toString } description={ group.englishDescription }/>)
      }
    </food-groups>

  def parseXml(root: NodeSeq) = {
    (root \ "group").map(group => {
      val id = group.attribute("id").get.text.toInt
      val description = group.attribute("description").get.text
      
      FoodGroup(id, description, Some(description))
    })
  }
}