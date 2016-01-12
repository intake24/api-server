/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.phrasesearch.tests

import org.scalatest.FunSuite
import org.workcraft.phrasesearch.CaseInsensitiveString
import org.workcraft.phrasesearch.InterpretedWord
import org.workcraft.phrasesearch.InterpretedPhrase
import org.workcraft.phrasesearch.WordInterpretation
import org.workcraft.phrasesearch.Exact
import org.workcraft.phrasesearch.AltSpelling
import org.workcraft.phrasesearch.Lev1
 
class InterpretedPhraseTests extends FunSuite {
  import InterpretedPhrase._
  
  val iw1 = InterpretedWord("blah", Exact("blah") +: Vector("bloh", "bluh", "bleh").map(AltSpelling(_, Lev1)))
  val phrase1 = InterpretedPhrase ("blah", IndexedSeq(iw1, iw1, iw1, iw1))
  val phrase2 = InterpretedPhrase ("blah", IndexedSeq(iw1))
  val phrase3 = InterpretedPhrase ("blah", IndexedSeq(iw1, iw1))
  
  test ("count combinations") {
    assert (countCombinations(phrase1.words) === 256)
    assert (phrase1.generateCombinations(500).length === countCombinations(phrase1.words))
  }
  
  test ("generate combinations") {
    assert (phrase2.generateCombinations(500) === List(List(0), List(1), List(2), List(3)))
    assert (phrase3.generateCombinations(500) === List(List(0, 0), List(0, 1), List(0, 2), List(0, 3),
    												   List(1, 0), List(1, 1), List(1, 2), List(1, 3),
    												   List(2, 0), List(2, 1), List(2, 2), List(2, 3),
    												   List(3, 0), List(3, 1), List(3, 2), List(3, 3)))
  }
  
  test("cut combinations") {
    intercept[IllegalArgumentException] {
      cutCombinations(phrase1.words, 0)
    }
    
    assert (countCombinations(cutCombinations(phrase1.words, 100)) <= 100)
    assert (countCombinations(cutCombinations(phrase1.words, 1)) === 1)
  } 
}