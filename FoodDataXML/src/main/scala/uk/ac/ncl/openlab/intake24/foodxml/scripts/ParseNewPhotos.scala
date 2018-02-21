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

object ParseNewPhotos {

  /*case class Row(description: String, tp: String, weight: String, id: String, foods: Seq[String])

  def commonPrefix(s1: String, s2: String) =
    {
      def rec(rem1: List[Char], rem2: List[Char], res: List[Char]): List[Char] =
        if (rem1.isEmpty || rem2.isEmpty || rem1.head != rem2.head)
          res.reverse
        else rec(rem1.tail, rem2.tail, rem1.head :: res)

      rec(s1.toList, s2.toList, List()).mkString
    }

  def mkid(photoIds: Seq[String]): String = photoIds.reduceLeft(commonPrefix)
  
  def locatePhoto (filename: String, baseDir: String ) = {
    println ("locating " + filename)
    val dir = new File(baseDir)
    val files = dir.listFiles(new FileFilter {def accept (f: File) = f.isFile})
    if (files.exists(_.getName() == filename)) 
      filename
      else dir.listFiles(new FileFilter{ def accept (f: File) = f.isDirectory}).find(_.listFiles().exists(_.getName == filename)).get.getName + "/" + filename
  }

  def main(args: Array[String]): Unit = {
    val sourcePath = "D:\\SCRAN24\\Notes\\new_photos.csv"
    val asServedSrc = "D:\\SCRAN24\\Data\\as-served.xml"
    val asServedDst = "D:\\SCRAN24\\Data\\as-served-new.xml"
    val foodsSrc = "D:\\SCRAN24\\Data\\foods.xml"
    val foodsDst = "D:\\SCRAN24\\Data\\foods-newimg.xml"
    val photosBase = "D:\\SCRAN24\\Photos_new"

      
    //FOOD (for reference)	PHOTO TYPE	WEIGHT (g)	photo code	food code(s) linked to																																															
    val rows = new CSVReader(new FileReader(sourcePath)).readAll().toSeq.map(_.toSeq)

    val parsedRows = rows.tail.map(r => Row(r(0), r(1), r(2), r(3), r.drop(4).filterNot(_.isEmpty).filter(_.length == 4)))

    val grouped = parsedRows.groupBy(r => (r.description, r.tp))
    
    val info = grouped.keySet.filter(k => k._2 == "AS" || k._2 == "LO").toSeq.map ( k => {
      val rows = grouped(k)
      val id = mkid(rows.map(_.id)) + (if (k._2 == "LO") "_leftovers" else "")
      val images = rows.map(r => AsServedImage(locatePhoto(r.id + ".jpg", photosBase), r.weight.toDouble))
      (AsServedSet(id, rows.head.description, images), rows.head.foods.map( f => ((f, (k._2 == "LO")), id))) 
    })
    
    val (newAsServedSets, foodList) = info.unzip
    
    val foodMap = foodList.flatten.toMap
    
    val asServedSets = AsServedDef.parseXml(XML.load(asServedSrc))
    
    Util.writeXml(AsServedDef.toXml(asServedSets.values.toSeq ++ newAsServedSets), asServedDst)
        
    val foods = FoodDef.parseXml(XML.load(foodsSrc))
    
    val updatedFoods = foods.map ( f => foodMap.get((f.code, false)) match {
      case Some(imageSetId) => {
        println (s"Updating portion size for $f.code")
        println (s"Previous portion size: ${f.portionSize.toString}")
        
        val lo = foodMap.get((f.code, true))
        
        f.copy (portionSize = Seq(PortionSizeMethod("as-served", "no description", "portion/placeholder.jpg", Seq(("serving-image-set", imageSetId)) ++ lo.map(("leftovers-image-set", _)) )))
      }
      case None => f
    })
    
    Util.writeXml(FoodDef.toXml(updatedFoods), foodsDst)
  }*/
}