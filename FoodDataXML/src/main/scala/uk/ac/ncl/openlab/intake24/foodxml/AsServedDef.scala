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

import uk.ac.ncl.openlab.intake24.api.data.AsServedHeader

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{Attribute, NodeSeq, Null, Text}

case class AsServedImageV1(url: String, weight: Double)

case class AsServedSetV1(id: String, description: String, images: Seq[AsServedImageV1]) {
  def toHeader = AsServedHeader(id, description)
}


object AsServedDef {
  def toXml(sets: Iterable[AsServedSetV1]) =
    <as-served-sets>
      {sets.toSeq.sortBy(_.id).map(set =>
      <as-served-set>
        {set.images.map(img =>
          <as-served-image/>
          % Attribute(None, "url", Text(img.url), Null)
          % Attribute(None, "weight", Text(img.weight.toString), Null))}
      </as-served-set> % Attribute(None, "id", Text(set.id), Null) % Attribute(None, "description", Text(set.description), Null))}
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