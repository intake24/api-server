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

package uk.ac.ncl.openlab.intake24.services.foodindex.english

import org.workcraft.phrasesearch.Metaphone3Encoder

import uk.ac.ncl.openlab.intake24.services.FoodDataService
import uk.ac.ncl.openlab.intake24.services.foodindex.AbstractFoodIndex

abstract class EnglishFoodIndex(foodData: FoodDataService, locale: String)
  extends AbstractFoodIndex(foodData, Some(Metaphone3Encoder()), EnglishStemmerPlingImpl(), EnglishFoodIndex.indexFilter,
    EnglishFoodIndex.nonIndexedWords, EnglishFoodIndex.specialFoodNames, locale)

object EnglishFoodIndex {
  val indexFilter = Seq("`", ",", """\/""", """\-""", """\)""", """\(""", """\.""", """e\.g\.""", """e\.g""", """\'s""")
  val nonIndexedWords = Seq("and", "the", "with", "from")
  val specialFoodNames = new EnglishSpecialFoodNames
}