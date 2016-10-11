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
import scala.xml.NodeSeq
import uk.ac.ncl.openlab.intake24.AsServedSetV1
import uk.ac.ncl.openlab.intake24.AsServedImageV1
import scala.xml.NodeSeq.seqToNodeSeq

object AsServedDef {
  def toXml(sets: Iterable[AsServedSetV1]) =
    <as-served-sets>
      {
        sets.toSeq.sortBy(_.id).map(set =>
          <as-served-set>
            {
              set.images.map(img =>
                <as-served-image/>
                  % Attribute(None, "url", Text(img.url), Null)
                  % Attribute(None, "weight", Text(img.weight.toString), Null))
            }
          </as-served-set> % Attribute(None, "id", Text(set.id), Null) % Attribute(None, "description", Text(set.description), Null))
      }
    </as-served-sets>

  def parseXml(root: NodeSeq) = {
    (root \ "as-served-set").map(set => {
      val as = (set \ "as-served-image").map(n => AsServedImageV1(n.attribute("url").get.text, n.attribute("weight").map(_.text.toDouble).get))

      val id = set.attribute("id").get.text
      val description = set.attribute("description").get.text

      (id, AsServedSetV1(id, description, as))
    }).toMap
  }
}