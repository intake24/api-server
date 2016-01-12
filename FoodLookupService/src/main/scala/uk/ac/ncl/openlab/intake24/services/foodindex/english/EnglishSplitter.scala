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

abstract class EnglishSplitter (foodData: FoodDataService, locale: String) extends Splitter {
  val splitList = foodData.splitList(locale)
  
  val pairsWithDefault = splitList.pairs.withDefaultValue(Set())
  
  val log = LoggerFactory.getLogger(classOf[EnglishSplitter])
  
  log.debug(splitList.toString())
  
  val pattern = ("""(?i)([^\s]+)\s+(""" + splitList.splitOnWords.map(Pattern.quote(_)).mkString("|") + """)\s+([^\s]+)""").r

  def split(description: String): List[String] = {
    @tailrec
    def rec(prefix: String, remainder: String, parts: List[String]): List[String] = pattern.findFirstMatchIn(remainder) match {
      case Some(m) => {
        val startOfSecondWord = m.end - m.group(3).length
        val nextRemainder = remainder.substring(startOfSecondWord)

        val pairSet = pairsWithDefault(m.group(1).toLowerCase)

        if (pairSet.contains(m.group(3).toLowerCase) || pairSet.contains("*"))
          rec(prefix + remainder.substring(0, startOfSecondWord), nextRemainder, parts)
        else
          rec("", nextRemainder, (prefix + remainder.substring(0, m.start + m.group(1).length)) :: parts)
      }
      case None => (prefix + remainder) :: parts
    }

    val d = description.toLowerCase

    if (d.contains("sand") || d.contains("salad"))
      List(description)
    else
      rec("", description, List()).reverse
  }
}

/* object SplitterImpl {
  def makePairs(splitWords: List[String], entries: Seq[IndexEntry]) = {
    val pattern = ("""(?i)([^\s]+)\s+(""" + splitWords.map(Pattern.quote(_)).mkString("|") + """)\s+([^\s]+)""").r

    def allPairs(s: String) = {
      @tailrec
      def rec(remainder: String, acc: List[(String, String)]): List[(String, String)] = pattern.findFirstMatchIn(remainder) match {
        case Some(m) => rec(remainder.substring(m.end - m.group(3).length), (m.group(1), m.group(3)) :: acc)
        case _ => acc
      }
      rec(s, List())
    }

    val z = Map[String, Set[String]]().withDefaultValue(Set())
    entries.map(_.description).foldLeft(z)((map, next) => allPairs(next.replaceAll(",", "").toLowerCase).foldLeft(map) { case (m, (w1, w2)) => m + (w1 -> (m(w1) + w2)) })
  }

  def main(args: Array[String]) = {
    val s = new Splitter(SplitList.parseFile("D:\\SCRAN24\\Data\\split_list"))

    println(s.split("chicken and rice"))
    println(s.split("sweet and sour chicken and rice"))
    println(s.split("chips and fish"))
    println(s.split("fIsH and cHiPs"))
    println(s.split("sour and sweet chicken"))
    println(s.split("sweet and sour chicken"))
    println(s.split("sweEt and sour Chicken and Fish and chips"))
  }
} */