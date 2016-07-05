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

import java.io.FileReader
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.AssociatedFood
import scala.xml.XML
import au.com.bytecode.opencsv.CSVWriter
import java.io.FileWriter
import java.io.File
import uk.ac.ncl.openlab.intake24.foodxml.PromptDef

object WritePrompts {
  
  val left = Array("any", "a")
  
  val right = Array("on", "in", "with")
  
  def extractGenericName(promptText: String) = {
    val words = promptText.split("\\s+")
    
    val leftIndex = words.indexWhere { left.contains(_) }
    val rightIndex = words.indexWhere { right.contains(_) }
    
    println ("Words:" + words.mkString(","))
    println ("Left: " + leftIndex + " right: " + rightIndex)
    
    if (leftIndex > 0 && rightIndex > 0)
      Some(words.slice(leftIndex+1, rightIndex).mkString(" "))
    else
      None
  }
  
  def main(args: Array[String]): Unit = {
    val destPath = "/home/ivan/tmp/prompts.csv"

    // short_code | is main | associated food code | prompt text ...

    val writer = new CSVWriter(new FileWriter(new File(destPath)))

    writer.writeNext(Array("Food code", "Link as main food?", "Associated food code", "Prompt text", "Generic food name"))

    val prompts = PromptDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/prompts.xml"))

    prompts.keySet.toSeq.sorted.foreach { key =>
      prompts(key).foreach { prompt =>
        writer.writeNext(Array(key, prompt.linkAsMain.toString, prompt.category, prompt.promptText, extractGenericName(prompt.promptText).getOrElse("???") ))
      }
    }
    
    writer.close

  }
}