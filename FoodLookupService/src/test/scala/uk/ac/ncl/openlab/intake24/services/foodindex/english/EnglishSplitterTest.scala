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

import org.scalatest.FunSuite
import net.scran24.fooddef.SplitList

class EnglishSplitterTest extends FunSuite {
  
  val splitWords = Seq("and", "with")   
  
  val ignorePairs = Map[String, Set[String]](
      "sweet" -> Set("sour"),
      "qweqwe" -> Set("*")
      )
  
  val splitList = SplitList(splitWords, ignorePairs)
  
  val splitter = EnglishSplitter(splitList)  
  
  test ("Empty input") {
    assert(splitter.split("") === Seq(""))  
  }
  
  test ("Single split word") {
    assert (splitter.split("fish and chips") === Seq("fish", "chips"))    
  }
  
  test ("Repeated split word") {
    assert (splitter.split("fish and and with chips and   and   dog") === Seq("fish", "chips", "dog"))
  }
  
  test ("Keep pair") {
    assert (splitter.split("sweet and sour chicken") === Seq("sweet and sour chicken"))
  }
  
  test ("Keep pair wildcard") {
    assert (splitter.split("qweqwe with 123") === Seq("qweqwe with 123"))
  }
  
  test ("Commas and ampersands") {
    assert (splitter.split("x,y & z") === Seq("x", "y", "z"))
  }
  
  test ("Multiple commas in a row") {
    assert(splitter.split("x,,y,,,z") === Seq("x", "y", "z"))  
  }
  
  test("Mixed split words and characters") {
    assert(splitter.split("x, sweet and sour chicken with y &&& z") === Seq("x", "sweet and sour chicken", "y", "z"))
  }
  
}