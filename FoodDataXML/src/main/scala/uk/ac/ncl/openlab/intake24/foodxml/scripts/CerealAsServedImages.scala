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

import java.io.File
import java.io.FileFilter
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.AsServedImage
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import uk.ac.ncl.openlab.intake24.foodxml.Util


object CerealAsServedImages extends App {
  def asServedImages(prefix: String, bowl: String, baseDir: String) = {
    def number(f: File) =
      f.getName().drop(prefix.length() + bowl.length()).dropRight(4).toInt
    val dir = new File(baseDir)
    dir.listFiles(new FileFilter { def accept(f: File) = f.getName().startsWith(prefix + bowl) }).sortBy(number(_)).toSeq
  }

  val photoBaseDir = "D:\\SCRAN24\\Photos_new"
  val sourcePath = "D:\\SCRAN24\\Notes\\cereal_photos.csv"
  val baseDir = "D:\\SCRAN24\\Photos_new\\cereal"
  val srcSets = "D:\\SCRAN24\\Data\\as-served.xml"
  val dstSets = "D:\\SCRAN24\\Data\\as-served-cereal.xml"

  // FOOD (for reference) PHOTO TYPE	WEIGHT (g)	photo code	food code(s) linked to																																															
  val rows = new CSVReader(new FileReader(sourcePath)).readAll().toSeq.map(_.toSeq)

  val weights = rows.map(r => (r(3), r(2).toDouble)).toMap
  val photoType = rows.map(r => (r(3), r(1))).toMap

  val types = Seq("hoop", "flake", "rkris")
  val bowls = Seq("A", "B", "C", "D", "E", "F")

  def images(prefix: String, bowl: String, t: String): AsServedSet = {
    def photoCode(f: File) = f.getName().dropRight(4)
    val images = asServedImages(prefix, bowl, baseDir).filter(f => photoType(photoCode(f)) == t).map(f => AsServedImage(f.getPath.drop(photoBaseDir.length + 1).replaceAll("\\\\", "\\/"), weights(photoCode(f))))
    AsServedSet("cereal_" + prefix + bowl + (if (t == "LO") "_leftovers" else ""), "Cereal, " + prefix + ", bowl " + bowl + (if (t == "LO") ", leftovers" else ""), images)
  }

  val sets =
    for (
      ctype <- types;
      bowl <- bowls;
      imageType <- Seq("AS", "LO")
    ) yield images(ctype, bowl, imageType)

  Util.writeXml(AsServedDef.toXml((AsServedDef.parseXml(XML.load(srcSets)).values.toSeq ++ sets).sortBy(_.id)), dstSets)
}