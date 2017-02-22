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

package apidocs


import uk.ac.ncl.openlab.intake24._
import upickle.default._


object SharedExamples {

  import JSONPrettyPrinter._

  val version = java.util.UUID.fromString("454a02a5-785e-4ca8-af52-81b11c28f56e")

  val foodHeader = FoodHeader("FOOD001", "Example food", Some("Пример продукта"), false)

  val categoryHeader = CategoryHeader("CAT001", "Example category", Some("Пример категории"), false)

  val associatedFoods = Seq(AssociatedFoodWithHeader(Left(foodHeader), "Did you have spread on your Y?", false, "spread"),
    AssociatedFoodWithHeader(Right(categoryHeader), "Did you have milk in your Y?", true, "milk"))

  val brands = Seq("Brand X", "Brand Y")

  val food1 = FoodRecord(MainFoodRecord(version, "MLAS", "Meat lasagne (includes homemade)", 107, InheritableAttributes(Some(true), None, None), Seq(categoryHeader), Seq()),
    LocalFoodRecord(Some(version), Some("Лазанья с мясом (в том числе домашнего приготовления)"), false, Map("NDNS" -> "1348"), Seq(PortionSizeMethod("as-served", "Use an image", "portion/lasagne.jpg", false,
      Seq(PortionSizeMethodParameter("serving-image-set", "lasagne"),
        PortionSizeMethodParameter("leftovers-image-set", "lasagne_leftovers")))), associatedFoods, brands))

  val food1Json = asPrettyJSON(food1)

  val food1AttrJson = asPrettyJSON(food1.main.attributes)

  val food1PsmJson = asPrettyJSON(food1.local.portionSize(0))

  val cat = CategoryRecord(MainCategoryRecord(version, "BRED", "Bread/crumpets/pancakes/yorkshire puddings", false, InheritableAttributes(None, None, None), Seq(categoryHeader)),
    LocalCategoryRecord(Some(version), Some("Хлебобулочные изделия/блины/пудинги"), Seq()))

  val cat2 = CategoryRecord(MainCategoryRecord(version, "BSCT", "Biscuits & crackers", false, InheritableAttributes(None, None, None), Seq(categoryHeader)),
    LocalCategoryRecord(Some(version), Some("Печенье и крекеры"), Seq()))

  val catJson = asPrettyJSON(cat)

  val catHeader = CategoryHeader(cat.main.code, cat.main.englishDescription, cat.local.localDescription, cat.main.isHidden)

  val catHeaderJson = asPrettyJSON(catHeader)

  val foodHeader2 = FoodHeader(food1.main.code, food1.main.englishDescription, food1.local.localDescription, false)

  val foodHeaderJson = asPrettyJSON(foodHeader2)

  val catContents = CategoryContents(Seq(foodHeader), Seq(catHeader))

  val catContentsJson = asPrettyJSON(catContents)

  val rootCats = asPrettyJSON(Seq(cat, cat2))


  val newFoodRequest = asPrettyJSON(NewMainFoodRecord("F001", "New food", 1, InheritableAttributes(Some(true), None, None), Seq("CAT001", "CAT002"), Seq()))


  val newCategoryRequest = asPrettyJSON(NewCategory("C001", "New category", false, InheritableAttributes(Some(true), None, None)))


  val foodBase = asPrettyJSON(MainFoodRecord(version, "F001", "Updated food", 1, InheritableAttributes(Some(true), None, None), Seq(categoryHeader, categoryHeader), Seq()))

  val foodLocal = asPrettyJSON(food1.local)

  val categoryBase = asPrettyJSON(cat.main)

  val categoryLocal = asPrettyJSON(cat.local)

  val foodGroup = asPrettyJSON(FoodGroupRecord(FoodGroupMain(1, "White bread/rolls"), FoodGroupLocal(Some("Белый хлеб"))))
}