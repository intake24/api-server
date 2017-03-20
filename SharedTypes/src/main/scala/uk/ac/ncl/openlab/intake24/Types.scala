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

package uk.ac.ncl.openlab.intake24

import java.util.UUID

sealed trait IndexEntryOld {
  val description: String
  val code: String
  val path: String

  val fullCode = if (path.isEmpty) code else path + ":" + code
}

case class InheritableAttributes(readyMealOption: Option[Boolean], sameAsBeforeOption: Option[Boolean], reasonableAmount: Option[Int])

object InheritableAttributes {
  val readyMealDefault = false
  val sameAsBeforeDefault = false
  val reasonableAmountDefault = 1000
}

/* TODO: Move these to corresponding services */

case class PortionSizeMethodParameter(name: String, value: String)

case class PortionSizeMethod(method: String, description: String, imageUrl: String, useForRecipes: Boolean, parameters: Seq[PortionSizeMethodParameter])

case class AssociatedFood(foodOrCategoryCode: Either[String, String], promptText: String, linkAsMain: Boolean, genericName: String)

case class BrandName(id: Int, name: String)

/**/

case class FoodOld(code: String, description: String, isDrink: Boolean, ndnsCode: Int, path: String, portionSize: Seq[PortionSizeMethod]) extends IndexEntryOld

case class FoodRecord(main: MainFoodRecord, local: LocalFoodRecord) {
  def allowedInLocale(locale: String) = (main.localeRestrictions.isEmpty || main.localeRestrictions.contains(locale)) && !local.doNotUse
}

case class MainFoodRecord(version: UUID, code: String, englishDescription: String, groupCode: Int, attributes: InheritableAttributes,
                          parentCategories: Seq[CategoryHeader], localeRestrictions: Seq[String])

case class MainFoodRecordUpdate(baseVersion: UUID, code: String, englishDescription: String, groupCode: Int, attributes: InheritableAttributes,
                                parentCategories: Seq[String], localeRestrictions: Seq[String])

case class NewMainFoodRecord(code: String, englishDescription: String, groupCode: Int, attributes: InheritableAttributes, parentCategories: Seq[String], localeRestrictions: Seq[String]) {
  def toHeader = FoodHeader(code, englishDescription, None, false)
}

case class NewFoodAutoCode(englishDescription: String, groupCode: Int, attributes: InheritableAttributes)

case class LocalFoodRecord(version: Option[UUID], localDescription: Option[String], doNotUse: Boolean,
                           nutrientTableCodes: Map[String, String], portionSize: Seq[PortionSizeMethod], associatedFoods: Seq[AssociatedFoodWithHeader],
                           brandNames: Seq[String]) {
  def toUpdate = LocalFoodRecordUpdate(version, localDescription, doNotUse, nutrientTableCodes, portionSize, associatedFoods.map(_.toAssociatedFood), brandNames)
}

case class LocalFoodRecordUpdate(baseVersion: Option[UUID], localDescription: Option[String], doNotUse: Boolean,
                                 nutrientTableCodes: Map[String, String], portionSize: Seq[PortionSizeMethod], associatedFoods: Seq[AssociatedFood],
                                 brandNames: Seq[String])

case class NewLocalFoodRecord(localDescription: Option[String], doNotUse: Boolean,
                              nutrientTableCodes: Map[String, String], portionSize: Seq[PortionSizeMethod], associatedFoods: Seq[AssociatedFood],
                              brandNames: Seq[String])

case class FoodHeader(code: String, englishDescription: String, localDescription: Option[String], doNotUse: Boolean)

case class UserFoodHeader(code: String, localDescription: String)

case class CategoryV1(code: String, description: String, children: Map[String, IndexEntryOld], path: String) extends IndexEntryOld

case class CategoryV2(version: UUID, code: String, description: String, foods: Seq[String], subcategories: Seq[String], isHidden: Boolean, attributes: InheritableAttributes, portionSizeMethods: Seq[PortionSizeMethod])

case class CategoryHeader(code: String, englishDescription: String, localDescription: Option[String], isHidden: Boolean)

case class UserCategoryHeader(code: String, localDescription: String)

case class NewCategory(code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes) {
  def toHeader = CategoryHeader(code, englishDescription, None, isHidden)
}

case class CategoryRecord(main: MainCategoryRecord, local: LocalCategoryRecord)

case class NewMainCategoryRecord(code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes, parentCategories: Seq[String]) {
  def toNewCategory = NewCategory(code, englishDescription, isHidden, attributes)
}

case class MainCategoryRecord(version: UUID, code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes,
                              parentCategories: Seq[CategoryHeader]) {
  def toUpdate = MainCategoryRecordUpdate(version, code, englishDescription, isHidden, attributes, parentCategories.map(_.code))
}

case class MainCategoryRecordUpdate(baseVersion: UUID, code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes,
                                    parentCategories: Seq[String])


case class LocalCategoryRecord(version: Option[UUID], localDescription: Option[String], portionSize: Seq[PortionSizeMethod]) {
  def toUpdate = LocalCategoryRecordUpdate(version, localDescription, portionSize)
}

case class NewLocalCategory(localDescription: Option[String])

case class NewLocalCategoryRecord(localDescription: Option[String], portionSize: Seq[PortionSizeMethod])

case class LocalCategoryRecordUpdate(baseVersion: Option[UUID], localDescription: Option[String], portionSize: Seq[PortionSizeMethod])

case class CategoryContents(foods: Seq[FoodHeader], subcategories: Seq[CategoryHeader])

case class UserCategoryContents(foods: Seq[UserFoodHeader], subcategories: Seq[UserCategoryHeader])


case class SplitList(splitWords: Seq[String], keepPairs: Map[String, Set[String]])

case class AsServedImageV1(url: String, weight: Double)

case class AsServedHeader(id: String, description: String)

case class AsServedSetV1(id: String, description: String, images: Seq[AsServedImageV1]) {
  def toHeader = AsServedHeader(id, description)
}

case class GuideHeader(id: String, description: String)

case class GuideImage(id: String, description: String, weights: Seq[GuideImageWeightRecord])

case class GuideImageWeightRecord(description: String, objectId: Int, weight: Double)

case class VolumeFunction(samples: Seq[(Double, Double)]) {
  if (samples.isEmpty)
    throw new IllegalArgumentException("samples cannot be empty")

  val sortedSamples = samples.sortBy(_._1)

  def asArray = {
    val res = new Array[Array[Double]](2)

    res(0) = new Array[Double](sortedSamples.size)
    res(1) = new Array[Double](sortedSamples.size)

    samples.indices.foreach(i => {
      res(0)(i) = sortedSamples(i)._1
      res(1)(i) = sortedSamples(i)._2
    })

    res
  }

  def apply(height: Double) = {
    def interp(lesser: (Double, Double), greaterOrEqual: (Double, Double)) = {
      val a = (height - lesser._1) / (greaterOrEqual._1 - lesser._1)
      lesser._2 + (greaterOrEqual._2 - lesser._2) * a
    }

    def rec(prev: (Double, Double), rest: Seq[(Double, Double)]): Double = {
      if (rest.head._1 >= height)
        interp(prev, rest.head)
      else if (rest.tail.isEmpty)
        rest.head._2
      else rec(rest.head, rest.tail)
    }

    if (height < 0.0)
      0.0
    else
      rec((0.0, 0.0), sortedSamples)
  }
}

case class VolumeSample(fl: Double, v: Double)

case class DrinkScale(objectId: Int, baseImagePath: String, overlayImagePath: String, width: Int, height: Int, emptyLevel: Int, fullLevel: Int, volumeSamples: Seq[VolumeSample])

case class DrinkwareHeader(id: String, description: String)

case class DrinkwareSet(id: String, description: String, guideId: String, scales: Seq[DrinkScale])


case class AssociatedFoodWithHeader(foodOrCategoryHeader: Either[FoodHeader, CategoryHeader], promptText: String, linkAsMain: Boolean, genericName: String) {
  def toAssociatedFood = {
    val foodOrCategoryCode = foodOrCategoryHeader.left.map(_.code).right.map(_.code)
    AssociatedFood(foodOrCategoryCode, promptText, linkAsMain, genericName)
  }
}

case class FoodGroupMain(id: Int, englishDescription: String)

case class FoodGroupLocal(localDescription: Option[String])

case class FoodGroupRecord(main: FoodGroupMain, local: FoodGroupLocal)

case class NutrientTable(id: String, description: String)

case class NutrientType(id: Int, description: String)

case class NutrientUnit(id: Int, symbol: String)

case class FoodCompositionRecord(table_id: String, record_id: String, nutrients: Map[Long, Double])

case class NutrientData(nutrient_id: Int, unitsPer100g: Double, unitName: String)

case class Locale(id: String, englishName: String, localName: String, respondentLanguage: String, adminLanguage: String, flagCode: String, prototypeLocale: Option[String])
