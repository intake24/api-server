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

package org.workcraft.phrasesearch

sealed abstract class WordInterpretation {
  def image: CaseInsensitiveString
}

sealed trait CorrectionMethod

object Phonetic extends CorrectionMethod {
  override def toString = "Phonetic"
}

object Lev1 extends CorrectionMethod {
  override def toString = "Levenshtein distance 1"
}

object Lev2 extends CorrectionMethod {
  override def toString = "Levenshtein distance 2"
}

case class Exact(val image: CaseInsensitiveString) extends WordInterpretation

case class AltSpelling(val image: CaseInsensitiveString, val method: CorrectionMethod) extends WordInterpretation

case class Synonym(val image: CaseInsensitiveString) extends WordInterpretation

case class InterpretedWord(asTyped: String, interpretations: IndexedSeq[WordInterpretation]) {
  if (interpretations.length > DefaultPhraseIndexConstants.maxWordInterpretations) throw new IllegalArgumentException("Too many interpretations. Too scary to proceed =(")

  def dropInterpretation = {
    // first try dropping some synonym
    val syn = interpretations.indexWhere { case Synonym(_) => true; case _ => false }
    if (syn > -1) {
      val (a, b) = interpretations.splitAt(syn)
      InterpretedWord(asTyped, a ++ b.tail)
    } else {
      // no synonyms, try dropping alternative spelling
      val altsp = interpretations.indexWhere { case AltSpelling(_, _) => true; case _ => false }
      if (altsp > -1) {
        val (a, b) = interpretations.splitAt(altsp)
        InterpretedWord(asTyped, a ++ b.tail)
      } else
      // must keep at least one interpretation
        throw new IllegalStateException("Cannot drop the only remaining word interpretation")
    }
  }
}

case class InterpretedPhrase(asTyped: String, words: IndexedSeq[InterpretedWord]) {

  import InterpretedPhrase._

  def generateCombinations(maxCombinations: Int) = {
    val workingSet = cutCombinations(words, maxCombinations)

    def prod[A](a: List[List[A]], b: List[A]) = for (i <- a; j <- b) yield j :: i

    val l = workingSet.map(_.interpretations.indices.toList).toList

    l match {
      case Nil => Nil
      case x :: Nil => x.map(List(_))
      case x :: y :: xs => xs.foldLeft(prod(x.map(List(_)), y))(prod(_, _)).map(_.reverse)
    }
  }
}

object InterpretedPhrase {
  def countCombinations(words: IndexedSeq[InterpretedWord]) = words.map(_.interpretations.length).foldLeft(1)(_ * _)

  def cutCombinations(words: IndexedSeq[InterpretedWord], maxCombinations: Int): IndexedSeq[InterpretedWord] = {
    val count = countCombinations(words)

    if (maxCombinations < 1)
      throw new IllegalArgumentException("maxCombinations must be at least 1")

    if (count > maxCombinations) {
      val maxInterpretations = words.indices.maxBy(i => words(i).interpretations.length)
      cutCombinations(words.updated(maxInterpretations, words(maxInterpretations).dropInterpretation), maxCombinations)
    } else
      words
  }
}

case class InterpretedPhrasePermutation(phrase: InterpretedPhrase, interpretationIndices: IndexedSeq[Int])

case class WordMatch(phrase_id: Int, wordIndex: Int, interpretationIndex: Int, pos: Int)

case class PhraseMatch[T](index: Int, value: T, wordCount: Int, quality: Int, words: List[WordMatch])

case class DictionaryPhrase(asTyped: String, words: Seq[CaseInsensitiveString])

class PhraseIndex[T](phrases: Seq[(String, T)], indexFilter: Seq[CaseInsensitiveString],
                     nonIndexedWords: Seq[CaseInsensitiveString], val phoneticEncoder: Option[PhoneticEncoder], val stemmer: WordOps, synsets: Seq[Set[CaseInsensitiveString]]) {

  def mkWordList(phrase: String) =
  // filter sequences of ignored character sequences and words such as 'e.g.' '/' '-' ',' 'and' 'with'
    indexFilter.foldLeft(phrase.toLowerCase()) { case (phrase, seq) => phrase.replaceAll(seq.lowerCase, " ") }
      // make a list of words
      .split("\\s+")
      // drop words that are too short
      .filter(_.length > 1)
      .map(CaseInsensitiveString(_))
      // split compound words (e.g. for German and Nordic languages)
      .flatMap(stemmer.splitCompound(_))
      .map(stemmer.stem(_))
      .filterNot(nonIndexedWords.contains(_))
      .toIndexedSeq

  val (desc, values) = phrases.unzip

  val (phraseList, dictionaryWords) = desc.foldLeft((List[DictionaryPhrase](), Set[CaseInsensitiveString]())) {
    case ((phrases, dict), phrase) =>
      val words = mkWordList(phrase)
      (DictionaryPhrase(phrase, words) :: phrases, dict ++ words)
  }

  val dictionary = new RichDictionary(dictionaryWords ++ synsets.flatten, phoneticEncoder, synsets)

  val phraseIndex = phraseList.reverse.zipWithIndex.map(_.swap).toMap

  val valueIndex = values.zipWithIndex.map(_.swap).toMap

  val wordIndex = phraseIndex.foldLeft(Map[CaseInsensitiveString, List[(Int, Int)]]().withDefaultValue(List[(Int, Int)]())) {
    case (map, (id, DictionaryPhrase(_, words))) =>
      words.zipWithIndex.foldLeft(map) {
        case (map, (word, pos)) =>
          map + (word -> ((id, pos) :: map(word)))
      }
  }

  def orderViolations(order: List[Int]) =
    order.indices.map(i => {
      val (left, right) = order.splitAt(i)
      val ref = right.head
      left.count(_ > ref) + right.count(_ < ref)
    }).foldLeft(0)(_ + _)

  def distanceViolations(order: List[Int]) =
    order.zip(order.drop(1)).map(x => math.abs(x._1 - x._2) - 1).foldLeft(0)(_ + _)


  def matchQuality(order: List[Int], orderViolationCost: Int, distanceCost: Int) =
    orderViolations(order) * orderViolationCost + distanceViolations(order) * distanceCost


  def findMatches(phrase: InterpretedPhrase, maxMatches: Int, maxCombinations: Int) = {
    def matchCombination(combination: List[Int]) = {
      // need to avoid matching a single input word with many phrase words at once,
      // but still match the same word multiple times in correct sequence if it is 
      // indeed seen several times in the input
      //
      // e.g. for dictionary phrase "a black dog and a black cat"
      // for input "black" we should only match "black" once
      // but for input "black black" we need to match both instances of "black"
      // and in the same order as they appear in the dictionary phrase

      val z = (List[WordMatch](), Set[(Int, Int)]())

      val wordMatches = {
        combination.zipWithIndex.foldLeft(z) {
          case ((wordMatches, usedMatches), (int_id, word_id)) => {
            val indexMatches = wordIndex(phrase.words(word_id).interpretations(int_id).image)

            // exclude words that have already been matched and take the first
            // (position-wise, from left to right) matching word 
            val newMatches = indexMatches.filterNot(usedMatches.contains(_)).groupBy(_._1).mapValues(_.map(_._2).min)

            (wordMatches ++ newMatches.map { case (phrase_id, pos_id) => WordMatch(phrase_id, word_id, int_id, pos_id) }, usedMatches ++ newMatches)
          }
        }._1
      }

      wordMatches.groupBy(_.phrase_id).toList.map {
        case (phrase_id, words) =>
          PhraseMatch(phrase_id, valueIndex(phrase_id), words.length,
            matchQuality(words.map(_.pos),
              DefaultPhraseIndexConstants.orderCost,
              DefaultPhraseIndexConstants.distanceCost) + (phraseIndex(phrase_id).words.length - words.length) * DefaultPhraseIndexConstants.unmatchedWordCost,
            words)
      }
    }

    phrase.generateCombinations(maxCombinations).flatMap(matchCombination(_)) match {
      case Nil => Nil
      case phraseMatches => {
        val bestMatch = phraseMatches.maxBy(_.wordCount).wordCount
        phraseMatches.filter(_.wordCount == bestMatch)
          // exclude duplicates which could have appeared due to different
          // variants of spelling correction on the same word matching
          // different words in the same phrase
          // e.g. the word "cat" is not in the dictionary and is corrected
          // to "oat" and "eat"
          // then it will match the phrase "eat oats" twice
          .map(phr => (phr.index, phr)).toMap.toList.map(_._2)
          .sortBy(_.quality)
          .take(maxMatches)
      }
    }
  }

  def interpretPhrase(phrase: String, strategy: MatchStrategy) =
    InterpretedPhrase(phrase, mkWordList(phrase).take(DefaultPhraseIndexConstants.maxWordsPerPhrase).map(dictionary.interpretWord(_, DefaultPhraseIndexConstants.maxWordInterpretations, strategy)).filterNot(_.interpretations.isEmpty))

  def lookup(phrase: String, maxResults: Int): Seq[(T, Int)] = {
    val stage1interpretation = interpretPhrase(phrase, MatchFewer)
    val stage1result = findMatches(stage1interpretation, maxResults, DefaultPhraseIndexConstants.maxPhraseCombinations).map(m => (m.value, m.quality))
    if (stage1result.size <= DefaultPhraseIndexConstants.maxMatchesForMatchMore) {
      if (stage1interpretation.words.exists(_.interpretations.exists { case AltSpelling(_, _) => true; case _ => false }))
        findMatches(interpretPhrase(phrase, MatchMore), maxResults, DefaultPhraseIndexConstants.maxPhraseCombinations).map(m => (m.value, m.quality))
      else stage1result
    }
    else stage1result
  }
}
