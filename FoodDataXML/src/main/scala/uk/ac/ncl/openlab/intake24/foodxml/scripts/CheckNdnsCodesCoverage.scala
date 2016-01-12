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

import scala.xml.XML

object CheckNdnsCodesCoverage extends App {
/*  val scranToNdns = scala.io.Source.fromFile("D:\\SCRAN24\\ExtractedDB\\ndns_scran_code_correspondence.txt").getLines.toList.map ( s => {
    val Array(scranCode, ndnsCode) = s.split ("\\s+")
    (scranCode, ndnsCode.toInt)
  }).toMap
  
  val foods = FoodDef.parseXml(XML.load("D:\\SCRAN24\\Data\\foods.xml"))
  
  Util.flatten(foods, f => {
    if (!scranToNdns.contains(f.code)) {
      println (s"${f.fullCode} (${f.description})")       
    }
  })
  
  val foods = FoodDef.parseXml(XML.load("D:\\SCRAN24\\Data\\foods.xml"))
  
  println ("Foods with unknown NDNS code: ")
  foods.filter (_.ndnsCode == -1).sortBy(_.description).foreach( f => println (s"${f.code} (${f.description})")) */
}