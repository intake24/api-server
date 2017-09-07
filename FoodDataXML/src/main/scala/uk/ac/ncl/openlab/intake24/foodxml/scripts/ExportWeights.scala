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

package uk.ac.ncl.openlab.intake24.foodxml.scripts

import java.io.FileWriter

import au.com.bytecode.opencsv.CSVWriter
import uk.ac.ncl.openlab.intake24.AsServedSetV1
import uk.ac.ncl.openlab.intake24.foodxml.{AsServedDef, GuideImageDef}

import scala.collection.JavaConverters._
import scala.xml.XML


object ExportWeights extends App {
  val asServedSets = AsServedDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/as-served.xml"))
  val writer = new CSVWriter(new FileWriter("/home/ivan/tmp/scran24_asw.csv"))

  val guide = GuideImageDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/guide.xml"))

  def toCSV(set: AsServedSetV1) =
    set.images.map(ps => Array(set.description, ps.url, ps.url.replaceAll(".jpg", ""), ps.weight.toString))

  writer.writeAll((Array("Description", "Image URL", "Image ID", "Weight, g") +: asServedSets.values.toSeq.sortBy(_.description).flatMap(toCSV(_))).asJava)

  writer.close

  val guideCSV = guide.keys.toSeq.sorted.flatMap(k =>
    guide(k).weights.map(r => Array(r.description, k, r.objectId.toString, r.weight.toString)))

  val guideWriter = new CSVWriter(new FileWriter("/home/ivan/tmp/scran24_gw.csv"))
  guideWriter.writeAll((Array("Description", "Guide image ID", "Object ID", "Weight, g") +: guideCSV).asJava)
  guideWriter.close

}