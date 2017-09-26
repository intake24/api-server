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

import com.infiauto.datastr.auto.LevenshteinAutomaton
import com.infiauto.datastr.auto.DictionaryAutomaton
import scala.collection.JavaConverters._

sealed trait MatchStrategy

case object MatchFewer extends MatchStrategy
case object MatchMore extends MatchStrategy

class RichDictionary(val words: Set[CaseInsensitiveString], val phoneticEncoder: Option[PhoneticEncoder], val synsets: Seq[Set[CaseInsensitiveString]]) {
  private val dictAuto = new DictionaryAutomaton(words.toSeq.map(_.lowerCase).asJava)
  private val lev1Auto = new LevenshteinAutomaton(1)
  private val lev2Auto = new LevenshteinAutomaton(2)

  private val synonymIndex = synsets.foldLeft(Map[CaseInsensitiveString, IndexedSeq[CaseInsensitiveString]]()) {
    case (map, set) =>
      map ++ set.map(w => (w, (set - w).toIndexedSeq)).toMap
  }.withDefaultValue(IndexedSeq())

  val phoneticDict = phoneticEncoder.map {
    encoder =>
      val z = Map[CaseInsensitiveString, List[CaseInsensitiveString]]().withDefaultValue(List())

      words.foldLeft(z) {
        case (dict, word) =>
          encoder.encode(word).foldLeft(dict) {
            case (dict, encoding) => dict + (encoding -> (word :: dict(encoding)))
          }
      }
  }

  def synonyms(word: CaseInsensitiveString): IndexedSeq[CaseInsensitiveString] = synonymIndex(word)

  def exactMatch(word: CaseInsensitiveString): Boolean = words.contains(word)

  def phoneticMatch(word: CaseInsensitiveString) = phoneticEncoder match {
    case Some(encoder) => encoder.encode(word).map(phoneticDict.get(_)).flatten
    case None => Seq()
  }

  def lev1Match(word: CaseInsensitiveString) = lev1Auto.recognize(word.lowerCase, dictAuto).asScala.map(CaseInsensitiveString(_)).toSet
  def lev2Match(word: CaseInsensitiveString) = lev2Auto.recognize(word.lowerCase, dictAuto).asScala.map(CaseInsensitiveString(_)).toSet

  def transpose[A](xs: IndexedSeq[IndexedSeq[A]]): IndexedSeq[IndexedSeq[A]] = {
    val ne = xs.filter(_.nonEmpty)
    if (ne.isEmpty) IndexedSeq()
    else ne.map(_.head) +: transpose(ne.map(_.tail))
  }

  def fewerSpellingInterpretations(word: CaseInsensitiveString, maxInterpretations: Int) = {
    val phoneticMatches = phoneticMatch(word).toIndexedSeq.sorted
    if (!phoneticMatches.isEmpty) {
      phoneticMatches.take(maxInterpretations).map(AltSpelling(_, Phonetic))
    } else {
      val lev1matches = lev1Match(word).toIndexedSeq.sorted
      if (!lev1matches.isEmpty)
        lev1matches.take(maxInterpretations).map(AltSpelling(_, Lev1))
      else
        lev2Match(word).toIndexedSeq.take(maxInterpretations).map(AltSpelling(_, Lev2))
    }
  }

  def moreSpellingInterpretations(word: CaseInsensitiveString, maxInterpretations: Int) = {
    val phoneticMatches = phoneticMatch(word).map(AltSpelling(_, Phonetic)).toIndexedSeq.sortBy(_.image)
    val lev1matches = lev1Match(word).map(AltSpelling(_, Lev1)).toIndexedSeq.sortBy(_.image)
    val lev2matches = lev2Match(word).map(AltSpelling(_, Lev2)).toIndexedSeq.sortBy(_.image)

    (phoneticMatches ++ lev1matches ++ lev2matches).take(maxInterpretations)
  }

  def interpretWord(word: CaseInsensitiveString, maxInterpretations: Int, strategy: MatchStrategy) = {
    if (exactMatch(word)) {
      InterpretedWord(word.self, Exact(word) +: synonyms(word).take(maxInterpretations - 1).map(Synonym(_)))
    } else {
      val sp = strategy match {
        case MatchFewer => fewerSpellingInterpretations(word, maxInterpretations)
        case MatchMore => moreSpellingInterpretations(word, maxInterpretations)
      }

      val syn = transpose(sp.map(s => synonyms(s.image))).flatten.filterNot(w => sp.exists(_.image == w)).take(maxInterpretations - sp.length).map(Synonym(_))

      InterpretedWord(word.self, sp ++ syn)
    }
  }
}

object RichDictionary {
  def loadWords(path: String): Set[CaseInsensitiveString] = scala.io.Source.fromFile(path)(scala.io.Codec.UTF8).getLines().map(CaseInsensitiveString(_)).filterNot(_.isEmpty).toSet
  def createFromFile(path: String) = new RichDictionary(loadWords(path), Some(Metaphone3Encoder()), Seq())
}