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

package uk.ac.ncl.openlab.intake24.services.foodindex.english

import scala.annotation.tailrec
import java.util.regex.Pattern
import uk.ac.ncl.openlab.intake24.services.FoodDataService
import uk.ac.ncl.openlab.intake24.services.foodindex.Splitter
import org.slf4j.LoggerFactory
import net.scran24.fooddef.SplitList

case class EnglishSplitter (splitList: SplitList) extends Splitter {
  
  val pairsWithDefault = splitList.keepPairs.withDefaultValue(Set())
  
  val log = LoggerFactory.getLogger(classOf[EnglishSplitter])
  
  log.debug(splitList.toString())
  
  val charPattern = """[,&\.\/]""".r 
  
  val pattern = ("""(?i)([^\s]+)(?:\s+(?:""" + splitList.splitWords.map(Pattern.quote(_)).mkString("|") + """))+\s+([^\s]+)""").r
  
  def split(description: String): List[String] = {
    @tailrec
    def rec(prefix: String, remainder: String, parts: List[String]): List[String] = pattern.findFirstMatchIn(remainder) match {
      case Some(m) => {
        val startOfSecondWord = m.end - m.group(2).length
        val nextRemainder = remainder.substring(startOfSecondWord)

        val pairSet = pairsWithDefault(m.group(1).toLowerCase)

        if (pairSet.contains(m.group(2).toLowerCase) || pairSet.contains("*"))
          rec(prefix + remainder.substring(0, startOfSecondWord), nextRemainder, parts)
        else
          rec("", nextRemainder, (prefix + remainder.substring(0, m.start + m.group(1).length)) :: parts)
      }
      case None => (prefix + remainder) :: parts
    }

    val lowerCaseDescription = description.toLowerCase

    if (lowerCaseDescription.contains("sand") || lowerCaseDescription.contains("salad"))
      List(description)
    else {
      // replace all instances of split characters (, & etc) with the first split word surrounded by spaces (e.g. " and ")
      val descWithReplacedChars = charPattern.replaceAllIn(lowerCaseDescription, s" ${splitList.splitWords.head} ") 
      rec("", descWithReplacedChars, List()).reverse
    }
  }
}
