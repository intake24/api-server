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

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConverters._
import scala.xml.XML
import scala.xml.PrettyPrinter
import java.io.PrintWriter
import java.io.File

object ParseNewCategories {

  case class Row(scranCode: String, name1: String, subCatCode: String, name2: String, ssCatCode: String, name3: String, foods: Seq[String])

  case class Category(code: String, description: String, foods: Set[String], subcategories: Set[String])

  def main(args: Array[String]): Unit = {
    val sourcePath = "D:\\SCRAN24\\Notes\\categories.csv"
    //  val asServedPath = "D:\\SCRAN24\\Data\\as-served.xml"
    //  val asServedDestPath = "D:\\SCRAN24\\Data\\as-served-weight.xml"
    //  val asServedDef = AsServedDef.parseXml(XML.load(asServedPath))
    //  header: SCRAN code	Description	Sub categories 	Description	Sub-sub categories	Description	Foods

    val rows = new CSVReader(new FileReader(sourcePath)).readAll().asScala.map(_.toSeq)

    val parsedRows = rows.tail.map(r => Row(r(0), r(1), r(2), r(3), r(4), r(5), r.drop(6).filterNot(_.isEmpty()))).filterNot(_.scranCode.isEmpty())

    //parsedRows.foreach( println (_))

    val subsubc = parsedRows.filterNot(_.ssCatCode.isEmpty).map(r => Category(r.ssCatCode, r.name3, r.foods.toSet, Set())).map(c => (c.code, c)).toMap
    val subc = parsedRows.filterNot(_.subCatCode.isEmpty).map(r =>
      if (r.ssCatCode.isEmpty)
        Category(r.subCatCode, r.name2, r.foods.toSet, Set())
      else (Category(r.subCatCode, r.name2, Set(), Set(r.ssCatCode)))).groupBy(_.code).mapValues(_.reduceLeft((a, b) => Category(a.code, a.description, a.foods ++ b.foods, a.subcategories ++ b.subcategories)))

    val cat = parsedRows.map(r =>
      if (r.subCatCode.isEmpty)
        Category(r.scranCode, r.name1, r.foods.toSet, Set())
      else (Category(r.scranCode, r.name1, Set(), Set(r.subCatCode)))).groupBy(_.code).mapValues(_.reduceLeft((a, b) => Category(a.code, a.description, a.foods ++ b.foods, a.subcategories ++ b.subcategories)))

    val all = subsubc ++ subc ++ cat

    all.mapValues(s => s.copy(description = s.description.charAt(0) + s.description.substring(1).toLowerCase)).foreach(println(_))

    val xml = <scran24-food-categories>
                {
                  all.keySet.toSeq.sorted.map(k => {
                    val cat = all(k)
                    <category code={cat.code} description={cat.description.charAt(0) + cat.description.substring(1).toLowerCase}>
                  		{
                  		  cat.foods.toSeq.sorted.map( ch => <food code={ch}/>) ++ 
                  		  cat.subcategories.toSeq.sorted.map ( s => <subcategory code={s}/>)
                  		}
                    </category>
                  })
                }
              </scran24-food-categories>
    
    val pp = new PrettyPrinter(120, 2)
    
    val writer = new PrintWriter(new File("D:\\SCRAN24\\Data\\categories.xml"))
    writer.println("<?xml version='1.0' encoding='utf-8'?>")
    writer.println(pp.format(xml))
    writer.close
  }
}