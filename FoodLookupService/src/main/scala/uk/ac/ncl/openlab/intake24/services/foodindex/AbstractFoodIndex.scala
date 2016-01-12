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

import org.workcraft.phrasesearch.CaseInsensitiveString
import org.workcraft.phrasesearch.PhraseIndex
import org.workcraft.phrasesearch.MatchFewer
import org.slf4j.LoggerFactory
import org.workcraft.phrasesearch.InterpretedPhrase
import net.scran24.fooddef.Food
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.FoodHeader
import net.scran24.fooddef.CategoryHeader
import com.google.inject.Singleton
import com.google.inject.Inject
import com.google.inject.name.Named
import org.workcraft.phrasesearch.Metaphone3Encoder
import uk.ac.ncl.openlab.intake24.services.FoodDataService
import org.workcraft.phrasesearch.PhoneticEncoder
import org.workcraft.phrasesearch.WordStemmer

abstract class AbstractFoodIndex (foodData: FoodDataService, phoneticEncoder: Option[PhoneticEncoder], stemmer: WordStemmer, indexFilter: Seq[String], nonIndexedWords: Seq[String], localSpecialFoods: LocalSpecialFoodNames, locale: String) extends FoodIndex {

  val log = LoggerFactory.getLogger(classOf[AbstractFoodIndex])
  
  val ft0 = System.currentTimeMillis()
  
  val foodsWithLocalNames = foodData.allFoods(locale).filterNot(_.localDescription.isEmpty)
  
  log.debug(s"Foods loaded in ${System.currentTimeMillis() - ft0} ms")
  
  val ct0 = System.currentTimeMillis()
  
  val categoriesWithLocalNames = foodData.allCategories(locale).filterNot(_.localDescription.isEmpty)
  
  log.debug(s"Categories loaded in ${System.currentTimeMillis() - ct0} ms")

  val indexEntries = foodsWithLocalNames.map(f => (f.localDescription.get, FoodEntry(f))) ++ categoriesWithLocalNames.map(c => (c.localDescription.get, CategoryEntry(c)))

  val it0 = System.currentTimeMillis()
  
  val index = new PhraseIndex(indexEntries, indexFilter.map(CaseInsensitiveString(_)), nonIndexedWords.map(CaseInsensitiveString(_)), phoneticEncoder, stemmer, foodData.synsets(locale).map(_.map(CaseInsensitiveString(_))))
  
  log.debug(s"Indexing complete in ${System.currentTimeMillis() - it0} ms")
  
  def specialFoodMatches(interpretation: InterpretedPhrase): Seq[MatchedFood] =
    if (interpretation.words.exists(_.interpretations.exists(_.image == CaseInsensitiveString(localSpecialFoods.sandwich))))
      Seq(MatchedFood(FoodHeader(FoodIndex.specialFoodSandwich, "Build my sandwich »", Some(localSpecialFoods.buildMySandwichLabel)), 0))
    else if (interpretation.words.exists(_.interpretations.exists(_.image == CaseInsensitiveString(localSpecialFoods.salad))))
      Seq(MatchedFood(FoodHeader(FoodIndex.specialFoodSalad, "Build my salad »", Some(localSpecialFoods.buildMySaladLabel)), 0))
    else Seq()

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

    val specialFoods = specialFoodMatches(interpretation)
    
    log.debug(s"Special food matches: $specialFoods")
    
    IndexLookupResult( specialFoods ++ matchedFoods, matchedCategories)
  }
}