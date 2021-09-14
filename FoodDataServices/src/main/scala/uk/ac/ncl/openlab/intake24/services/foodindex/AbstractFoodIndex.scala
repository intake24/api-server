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

package uk.ac.ncl.openlab.intake24.services.foodindex

import org.slf4j.LoggerFactory
import org.workcraft.phrasesearch._
import uk.ac.ncl.openlab.intake24.api.data.UserFoodHeader
import uk.ac.ncl.openlab.intake24.services.util.DbErrorHandler

abstract class AbstractFoodIndex(foodData: FoodIndexDataService, phoneticEncoder: Option[PhoneticEncoder], wordOps: WordOps, indexFilter: Seq[String],
                                 nonIndexedWords: Seq[String], localSpecialFoods: LocalSpecialFoodNames, locale: String)
  extends FoodIndex with DbErrorHandler {

  val log = LoggerFactory.getLogger(classOf[AbstractFoodIndex])

  val ft0 = System.currentTimeMillis()

  val localSpecialFoodHeaders = Seq(UserFoodHeader(FoodIndex.specialFoodSalad, localSpecialFoods.saladDescription), UserFoodHeader(FoodIndex.specialFoodSandwich, localSpecialFoods.sandwichDescription))

  val indexableFoods = throwOnError(foodData.indexableFoods(locale)) ++ localSpecialFoodHeaders

  log.debug(s"${indexableFoods.size} indexable foods for $locale loaded in ${System.currentTimeMillis() - ft0} ms")

  val foodIndexEntries = indexableFoods.map {
    header =>
      (header.localDescription, header)
  }

  val ct0 = System.currentTimeMillis()

  val indexableCategories = throwOnError(foodData.indexableCategories(locale))

  log.debug(s"${indexableCategories.size} indexable categories for $locale loaded in ${System.currentTimeMillis() - ct0} ms")

  val categoryIndexEntries = indexableCategories.map {
    header =>
      (header.localDescription, header)
  }

  val it0 = System.currentTimeMillis()

  val indexFilterCis = indexFilter.map(CaseInsensitiveString(_))
  val nonIndexedWordsCis = nonIndexedWords.map(CaseInsensitiveString(_))
  val synsets = throwOnError(foodData.synsets(locale)).map(_.map(CaseInsensitiveString(_)))

  val foodIndex = new PhraseIndex(foodIndexEntries, indexFilterCis, nonIndexedWordsCis, phoneticEncoder, wordOps, synsets)

  val categoryIndex = new PhraseIndex(categoryIndexEntries, indexFilterCis, nonIndexedWordsCis, phoneticEncoder, wordOps, synsets)

  log.debug(s"Indexing complete in ${System.currentTimeMillis() - it0} ms")

  def lookup(description: String, maxFoods: Int, maxCategories: Int): IndexLookupResult = {
    log.debug(s"Lookup request: $description")
    val foodInterpretation = foodIndex.interpretPhrase(description, MatchFewer)
    val categoryInterpretation = categoryIndex.interpretPhrase(description, MatchFewer)
    log.debug(s"Interpretation of input as food name: ${foodInterpretation}")
    log.debug(s"Interpretation of input as category name: ${categoryInterpretation}")

    val t0 = System.currentTimeMillis()

    val matchedFoods = foodIndex.lookup(description, maxFoods).map {
      case (h, cost) => MatchedFood(h, cost.toDouble)
    }

    val matchedCategories = categoryIndex.lookup(description, maxCategories).map {
      case (h, cost) => MatchedCategory(h, cost.toDouble)
    }

    matchedCategories.foreach(c => log.debug(c.toString))

    log.debug("Lookup completed in " + (System.currentTimeMillis() - t0) + " ms")

    IndexLookupResult(matchedFoods, matchedCategories)
  }
}
