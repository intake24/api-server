/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.phrasesearch

trait PhraseIndexConstants {
  def maxWordsPerPhrase: Int
  def maxWordInterpretations: Int
  def maxPhraseCombinations: Int
  def maxPhraseMatches: Int
  def distanceCost: Int
  def orderCost: Int
}

object DefaultPhraseIndexConstants extends PhraseIndexConstants {
  val maxWordsPerPhrase = 10
  val maxWordInterpretations = 10
  val maxPhraseCombinations = 1000
  val maxMatchesForMatchMore = 3 
  val maxPhraseMatches = 6
  val distanceCost = 1
  val orderCost = 4
  val unmatchedWordCost = 8
}