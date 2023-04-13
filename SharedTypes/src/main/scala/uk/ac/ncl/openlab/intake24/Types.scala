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

case class DrinkwareSetRecord(id: String, description: String, guideId: String)

case class DrinkwareSet(id: String, description: String, guideId: String, scales: Seq[DrinkScale])

case class FoodGroupMain(id: Int, englishDescription: String)

case class FoodGroupLocal(localDescription: Option[String])

case class FoodGroupRecord(main: FoodGroupMain, local: FoodGroupLocal)

case class NutrientTable(id: String, description: String)

case class NutrientType(id: Int, description: String)

case class NutrientUnit(id: Int, symbol: String)

case class NutrientData(nutrient_id: Int, unitsPer100g: Double, unitName: String)

case class Locale(id: String, englishName: String, localName: String, respondentLanguage: String, adminLanguage: String, flagCode: String, prototypeLocale: Option[String], textDirection: String)

case class NutrientTableRecord(id: String, nutrientTableId: String, description: String, localDescription: Option[String])

case class NewNutrientTableRecord(id: String, nutrientTableId: String, description: String, localDescription: Option[String], nutrients: Map[Long, Double])
