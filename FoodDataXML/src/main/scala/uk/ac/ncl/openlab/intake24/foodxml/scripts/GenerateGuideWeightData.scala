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
import java.io.FilenameFilter
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import scala.xml.Attribute
import scala.xml.Text
import scala.xml.Null
import scala.xml.XML

object GenerateGuideWeightData {
  def main(args: Array[String]): Unit = {
    val jpegsdir = new File("D:\\SCRAN24\\Guide Images")
    val oldData = "D:\\SCRAN24\\\\ExtractedDB\\tblPhoto_Weights.txt"
    val sourcePath = "D:\\SCRAN24\\ExtractedDB\\tblPhoto_Weights_3.csv"

    val guideNames = jpegsdir.listFiles(new FilenameFilter {
      def accept(f: File, s: String): Boolean = s.endsWith(".jpg")
    }).map(f => f.getName().substring(0, f.getName().length - 4)).sorted

    // header: "description","?","photo_id","weight"
    val rows = new CSVReader(new FileReader(sourcePath)).readAll().toList.map(_.toSeq)
    
    val oldrows = new CSVReader(new FileReader(oldData)).readAll().toList.map(_.toSeq)

    val weightTable = rows.map(r => (r(2), r(3).toDouble, r(0)))
    
    val oldWeightTable = oldrows.map(r => (r(2), r(3).toDouble, r(0)))

    val w = guideNames.map(n => {
      val weightsForThisGuide = {
        val old = oldWeightTable.filter(m => m._1.startsWith(n) && m._1.length > n.length && m._1.charAt(n.length).isDigit)
        if (old.isEmpty) {
        	println("No data in old table for " + n)
        	weightTable.filter(m => m._1.startsWith(n) && m._1.length > n.length && m._1.charAt(n.length).isDigit)
        } else old
      }

      if (weightsForThisGuide.isEmpty)
       	println("No data for " + n)

      def getN(s: String) = s.substring(n.length).toInt
      
      val z = weightsForThisGuide.map(v => (getN(v._1), v._2, v._3))
      
      z.foreach ( x => {
        val y = z.filter( _._1 == x._1)
        if (y.length > 1) {
        //  println ("duplicate data: " + n + " " + y)
          if (!y.forall( k => k._2 == y.head._2))
            println ("discrepancy: " + n + " " + y)
        }
          
      })

      (n, z.distinct)
    })

    val xml = <scran24-guide-images>
                {
                  w.map {
                    case (name, weights) =>
                      <guide-image>
                        { weights.sortBy(_._1).map(w => <weight/> % Attribute(None, "id", Text(w._1.toString), Null) % Attribute(None, "value", Text(w._2.toString), Null)
                          % Attribute (None, "description", Text(w._3), Null)) }
                      </guide-image> % Attribute(None, "id", Text(name), Null)
                  }
                }
              </scran24-guide-images>
    
    XML.save("D:\\scratch\\guide-images.xml", xml);

  }
}