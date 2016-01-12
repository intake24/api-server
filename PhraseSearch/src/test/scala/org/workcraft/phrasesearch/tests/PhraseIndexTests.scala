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
import org.workcraft.phrasesearch.PhraseIndex
import org.workcraft.phrasesearch.PhraseIndexConstants
import org.workcraft.phrasesearch.DefaultPhraseIndexConstants
import org.workcraft.phrasesearch.Metaphone3Encoder
import org.workcraft.phrasesearch.WordStemmer
 
class PhraseIndexTests extends FunSuite {
  val phrases1 = List (("Roast chicken", 1), ("Chicken roast", 2))
  
  val identityStemmer = new WordStemmer { def stem(word: CaseInsensitiveString) = word } 
  
  test ("dont crash on single word input") {
    val index = new PhraseIndex(phrases1, Seq(), Seq(), Some(Metaphone3Encoder()), identityStemmer, Seq())
    assert (index.lookup("dog", 1) === Seq())
  }
  
  test("exact match") {
    val index = new PhraseIndex(phrases1, Seq(), Seq(), Some(Metaphone3Encoder()), identityStemmer, Seq())
    
    assert (index.lookup("Roast chicken", 1) === Seq((1,0)))
    assert (index.lookup("chicken roast", 2) === Seq((2, 0),(1, DefaultPhraseIndexConstants.orderCost * 2)))
  }
}