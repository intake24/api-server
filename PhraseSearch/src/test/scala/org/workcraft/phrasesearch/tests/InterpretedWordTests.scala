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
import org.workcraft.phrasesearch.Synonym

class InterpretedWordTests extends FunSuite {
  import InterpretedPhrase._

  val iw1 = InterpretedWord("blah", Exact("blah") +: Vector("bloh", "bluh").map(AltSpelling(_, Lev1)) :+ Synonym("bleh"))

  val drop1 = iw1.dropInterpretation

  test("must have dropped one interpretation") {
    assert(drop1.interpretations.length === 3)
  }

  test("must have dropped the synonym") {
    assert(drop1.interpretations.exists { case Synonym(_) => true; case _ => false } === false)
  }

  val drop2 = drop1.dropInterpretation

  test("must have dropped an alt spelling") {
    assert(drop2.interpretations.exists { case Exact(_) => true; case _ => false } === true)
  }

  test("must keep the exact spelling") {
    assert(drop2.dropInterpretation.interpretations === Vector(Exact("blah")))
  }

  test("must throw an exception if the last interpretation is attempted to be dropped") {
    intercept[IllegalStateException] {
      drop2.dropInterpretation.dropInterpretation
    }
  }
}