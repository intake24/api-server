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

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.phrasesearch.tests

import org.scalatest.FunSuite
import org.workcraft.phrasesearch.RichDictionary
import org.workcraft.phrasesearch.CaseInsensitiveString
import org.workcraft.phrasesearch.Synonym
import org.workcraft.phrasesearch.MatchFewer
import org.workcraft.phrasesearch.Metaphone3Encoder

class RichDictionaryTests extends FunSuite {
  val words = Set("Cat", "Dog", "Duck", "muesli").map(CaseInsensitiveString(_))
  val words2 = Set("Hi").map(CaseInsensitiveString(_))
  val words3 = Set("cola", "pepsi", "coke", "thing", "something", "ahahaha").map(CaseInsensitiveString(_))
  val syns = Seq(
    Set("cola", "pepsi", "coke").map(CaseInsensitiveString(_)),
    Set("thing", "something", "ahahaha").map(CaseInsensitiveString(_)))

  test("exact match") {
    val dict = new RichDictionary(words, Some(Metaphone3Encoder()), Seq())

    assert(dict.exactMatch(CaseInsensitiveString("cat")) === true)
    assert(dict.exactMatch(CaseInsensitiveString("dog")) === true)
    assert(dict.exactMatch(CaseInsensitiveString("duck")) === true)
    assert(dict.exactMatch(CaseInsensitiveString("duk")) === false)
    assert(dict.exactMatch(CaseInsensitiveString("")) === false)
  }

  test("phonetic match") {
    val dict = new RichDictionary(words, Some(Metaphone3Encoder()), Seq())

    assert(dict.phoneticMatch(CaseInsensitiveString("dawg")) === Set(CaseInsensitiveString("dog")))
    assert(dict.phoneticMatch(CaseInsensitiveString("duk")) === Set(CaseInsensitiveString("duck")))
    assert(dict.phoneticMatch(CaseInsensitiveString("kaht")) === Set(CaseInsensitiveString("cat")))
  }

  test("edit distance match") {
    val dict = new RichDictionary(words, Some(Metaphone3Encoder()), Seq())

    assert(dict.lev1Match(CaseInsensitiveString("ducc")) === Set(CaseInsensitiveString("duck")))
    assert(dict.lev1Match(CaseInsensitiveString("qucc")) === Set())
    assert(dict.lev2Match(CaseInsensitiveString("qucc")) === Set(CaseInsensitiveString("duck")))
    assert(dict.lev2Match(CaseInsensitiveString("doog")) === Set(CaseInsensitiveString("dog")))
    assert(dict.lev2Match(CaseInsensitiveString("museli")) === Set(CaseInsensitiveString("muesli")))
  }

  test("match empty word") {
    val dict = new RichDictionary(words2, Some(Metaphone3Encoder()), Seq())
    assert(dict.phoneticMatch(CaseInsensitiveString("")) === Set())
    assert(dict.lev1Match(CaseInsensitiveString("")) === Set())
    assert(dict.lev2Match(CaseInsensitiveString("")) === Set(CaseInsensitiveString("hi")))
  }

  test("synonyms for exact match") {
    val dict = new RichDictionary(words3, Some(Metaphone3Encoder()), syns)

    val int = dict.interpretWord(CaseInsensitiveString("сoke"), 10, MatchFewer)

    assert(int.interpretations.exists(_ == Synonym("cola")))
    assert(int.interpretations.exists(_ == Synonym("pepsi")))
  }

  test("synonyms for alt spelling") {
    val dict = new RichDictionary(words3, Some(Metaphone3Encoder()), syns)

    val int = dict.interpretWord(CaseInsensitiveString("koke"), 10, MatchFewer)

    assert(int.interpretations.exists(_ == Synonym("cola")))
    assert(int.interpretations.exists(_ == Synonym("pepsi")))
  }
}