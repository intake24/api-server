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
import uk.ac.ncl.openlab.intake24.UserFoodHeader

abstract class AbstractFoodIndex(foodData: FoodIndexDataService, phoneticEncoder: Option[PhoneticEncoder], wordOps: WordOps, indexFilter: Seq[String], nonIndexedWords: Seq[String], localSpecialFoods: LocalSpecialFoodNames, locale: String) extends FoodIndex {

  val log = LoggerFactory.getLogger(classOf[AbstractFoodIndex])

  val ft0 = System.currentTimeMillis()

  val localSpecialFoodHeaders = Seq(UserFoodHeader(FoodIndex.specialFoodSalad, localSpecialFoods.saladDescription), UserFoodHeader(FoodIndex.specialFoodSandwich, localSpecialFoods.sandwichDescription))
  // FIXME: Error handling
  val indexableFoods = foodData.indexableFoods(locale).right.get ++ localSpecialFoodHeaders

  log.debug(s"${indexableFoods.size} indexable foods for $locale loaded in ${System.currentTimeMillis() - ft0} ms")

  val ct0 = System.currentTimeMillis()

  // FIXME: Error handling
  val indexableCategories = foodData.indexableCategories(locale).right.get

  log.debug(s"${indexableCategories.size} indexable categories for $locale loaded in ${System.currentTimeMillis() - ct0} ms")

  val indexEntries = indexableFoods.map(f => (f.localDescription, FoodEntry(f))) ++ indexableCategories.map(c => (c.localDescription, CategoryEntry(c)))

  val it0 = System.currentTimeMillis()

  // FIXME: Error handling
  val index = new PhraseIndex(indexEntries, indexFilter.map(CaseInsensitiveString(_)), nonIndexedWords.map(CaseInsensitiveString(_)), phoneticEncoder, wordOps, foodData.synsets(locale).right.get.map(_.map(CaseInsensitiveString(_))))

  log.debug(s"Indexing complete in ${System.currentTimeMillis() - it0} ms")

  def lookup(description: String, maxResults: Int): IndexLookupResult = {
    log.debug("Lookup request: \"" + description + "\"")
    val interpretation = index.interpretPhrase(description, MatchFewer)
    log.debug("Interpretation of input \"" + description + "\": " + interpretation)

    val t0 = System.currentTimeMillis()
    val (matchedFoods, matchedCategories) = index.lookup(description, maxResults).foldLeft((Seq[MatchedFood](), Seq[MatchedCategory]())) {
      case ((foods, cats), next) => next match {
        case (FoodEntry(food), cost) => (MatchedFood(food, cost) +: foods, cats)
        case (CategoryEntry(category), cost) => (foods, MatchedCategory(category, cost) +: cats)
      }
    }
    log.debug("Lookup completed in " + (System.currentTimeMillis() - t0) + " ms")

    IndexLookupResult(matchedFoods, matchedCategories)
  }
}