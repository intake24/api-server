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
import java.io.PrintWriter

import scala.xml.XML
import uk.ac.ncl.openlab.intake24.foodxml._

object ShowAllFoods {

  def showHierarchical(foods: Map[String, IndexEntryOld], pw: PrintWriter) : Unit = {
  	pw.println ("<div style=\"margin-left: 40px;\">");
    foods.values.toList.sortBy(_.description).foreach {
    case f: FoodOld => pw.println ("<table><tr><td width=\"200px\"><strong>" + f.fullCode + "</strong></td><td>" + f.description + "</td></tr></table>");
    case f: CategoryV1 => {
      	 val title = f.description + " (" + f.fullCode + ") ";
      	pw.println (if (f.path.isEmpty) "<h1>" + title + "</h1>" else "<h2>" + title +"</h2>")
      	 showHierarchical(f.children, pw)
    }
    }
    pw.println ("</div>")
  }
  
  def showFlat (foods: Map[String, CategoryV1], pw: PrintWriter, sortByDesc: Boolean) = {
    val q = Util.flatten(foods, f => (f.description, f.fullCode, "<table><tr><td width=\"200px\"><strong>" + f.fullCode + "</strong></td><td>" + f.description + "</td></tr></table>"))
    if (sortByDesc)
    	q.sortBy(_._1).map(_._3).foreach(pw.println (_))
    else
      	q.sortBy(_._2).map(_._3).foreach(pw.println (_))
  }

  
  def main(args: Array[String]): Unit = {
    val inFoods = "D:\\SCRAN24\\Data\\foods.xml"
    val rootCat = FoodDefOld.parseXml(XML.load(inFoods))
    
    val outHierarchical = "D:\\SCRAN24\\Notes\\food list hierarchical.html"
    val outFlat1 = "D:\\SCRAN24\\Notes\\food list flat sorted desc.html"
    val outFlat2 = "D:\\SCRAN24\\Notes\\food list flat sorted code.html"
    
    val pw1 = new PrintWriter (new File(outHierarchical))
      
    pw1.println ("<html>\n<body style=\"font-size:70%;\">");
    showHierarchical(rootCat, pw1)
    pw1.println ("</html>\n</body>");
    pw1.close
    
    val pw2 = new PrintWriter (new File(outFlat1))
    pw2.println ("<html>\n<body style=\"font-size:70%;\">");
    showFlat(rootCat, pw2, true)
    pw2.println ("</html>\n</body>");
    pw2.close
    
    val pw3 = new PrintWriter (new File(outFlat2))
    pw3.println ("<html>\n<body style=\"font-size:70%;\">");
    showFlat(rootCat, pw3, false)
    pw3.println ("</html>\n</body>");
    pw3.close
    

  }
}