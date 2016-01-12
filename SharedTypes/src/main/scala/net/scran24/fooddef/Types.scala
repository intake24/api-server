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

package net.scran24.fooddef

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

case class FoodOld(code: String, description: String, isDrink: Boolean, ndnsCode: Int, path: String, portionSize: Seq[PortionSizeMethod]) extends IndexEntryOld 

case class Food(version: UUID, code: String, englishDescription: String, groupCode: Int, attributes: InheritableAttributes, localData: FoodLocal)

case class FoodBase(version: UUID, code: String, englishDescription: String, groupCode: Int, attributes: InheritableAttributes)

case class FoodLocal(version: Option[UUID], localDescription: Option[String], nutrientTableCodes: Map[String, String], portionSize: Seq[PortionSizeMethod])

case class FoodHeader(code: String, englishDescription: String, localDescription: Option[String])

case class CategoryV1(code: String, description: String, children: Map[String, IndexEntryOld], path: String) extends IndexEntryOld
 
case class CategoryV2(version: UUID, code: String, description: String, foods: Seq[String], subcategories: Seq[String], isHidden: Boolean, attributes: InheritableAttributes, portionSizeMethods: Seq[PortionSizeMethod])

case class CategoryHeader(code: String, englishDescription: String, localDescription: Option[String], isHidden: Boolean)

case class Category(version: UUID, code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes, localData: CategoryLocal)

case class CategoryBase(version: UUID, code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes)

case class CategoryLocal(version: Option[UUID], localDescription: Option[String], portionSize: Seq[PortionSizeMethod])

case class CategoryContents(foods: Seq[FoodHeader], subcategories: Seq[CategoryHeader])

case class FoodData(code: String, englishDescription: String, localDescription: Option[String], nutrientTableCodes: Map[String, String], groupCode: Int,
  portionSize: Seq[PortionSizeMethod], readyMealOption: Boolean, sameAsBeforeOption: Boolean, reasonableAmount: Int)

case class SplitList(splitOnWords: Seq[String], pairs: Map[String, Set[String]])
  
case class AsServedImage(url: String, weight: Double)

case class AsServedHeader (id: String, description: String)

case class AsServedSet (id: String, description: String, images: Seq[AsServedImage])

case class GuideHeader(id: String, description: String)

case class GuideImage (id: String, description: String, weights: Seq[GuideImageWeightRecord])

case class GuideImageWeightRecord (description: String, objectId: Int, weight: Double)

case class VolumeFunction (samples: Seq[(Double, Double)]) {
  if (samples.isEmpty)
    throw new IllegalArgumentException("samples cannot be empty") 
  
  val sortedSamples = samples.sortBy(_._1)
  
  def asArray = {
    val res = new Array[Array[Double]](2)
    
    res(0) = new Array[Double](sortedSamples.size)
    res(1) = new Array[Double](sortedSamples.size)
    
    samples.indices.foreach( i => {
      res(0)(i) = sortedSamples(i)._1
      res(1)(i) = sortedSamples(i)._2
    })
    
    res
  }
  
  def apply (height: Double) = {
    def interp (lesser: (Double, Double), greaterOrEqual: (Double, Double)) = {
      val a = (height - lesser._1) / (greaterOrEqual._1 - lesser._1)
      lesser._2 + (greaterOrEqual._2 - lesser._2) * a
    }
    
    def rec (prev: (Double, Double), rest: Seq[(Double, Double)]): Double = {
      if (rest.head._1 >= height)
        interp(prev, rest.head)
      else
        if (rest.tail.isEmpty)
          rest.head._2
        else rec (rest.head, rest.tail)
    }
    
    if (height < 0.0)
      0.0
    else
      rec ((0.0, 0.0), sortedSamples)
  } 
}

case class DrinkScale (choice_id: Int, baseImage: String, overlayImage: String, width: Int, height: Int, emptyLevel: Int, fullLevel: Int, vf: VolumeFunction)

case class DrinkwareHeader(id: String, description: String)

case class DrinkwareSet(id: String, description: String, guide_id: String, scaleDefs: Seq[DrinkScale])

case class PortionSizeMethodParameter (name: String, value: String)

case class PortionSizeMethod (method: String, description: String, imageUrl: String, useForRecipes: Boolean, parameters: Seq[PortionSizeMethodParameter])

case class Prompt (category: String, promptText: String, linkAsMain: Boolean, genericName: String)


case class FoodGroup (id: Int, englishDescription: String, localDescription: Option[String]) {
  override def toString = id.toString + ". " + englishDescription
}

case class NutrientTable(id: String, description: String)