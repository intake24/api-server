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

package uk.ac.ncl.openlab.intake24.services

import org.scalatest.FunSuite

import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.DrinkScale
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.GuideImageWeightRecord
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.VolumeFunction
import uk.ac.ncl.openlab.intake24.UserFoodData

abstract class UserFoodDataServiceTest extends FunSuite {

  val service: UserFoodDataService

  val defaultLocale = "en_GB"

  val c0 = UserCategoryHeader("C000", "Category 1")
  val c1 = UserCategoryHeader("C001", "Category 2")
  val c2 = UserCategoryHeader("C002", "Category 3")
  val c3 = UserCategoryHeader("C003", "Category 4")
  val c4 = UserCategoryHeader("C004", "Nested category 1")
  val c5 = UserCategoryHeader("C005", "Nested category 2") 

  test("Get root categories") {
    val expected = Seq(UserCategoryHeader("C000", "Category 1"), UserCategoryHeader("C001", "Category 2"),
      UserCategoryHeader("C002", "Category 3"), UserCategoryHeader("C003", "Category 4"))

    assert(service.rootCategories(defaultLocale) === expected)
  }

  test("Attribute & portion size method inheritance") {
    // Must inherit directly from C001
    val expectedPsm1 = Seq(
      PortionSizeMethod("as-served", "No description", "portion/placeholder.jpg", false, Seq(PortionSizeMethodParameter("serving-image-set", "as_served_2"), PortionSizeMethodParameter("leftovers-image-set", "as_served_2_leftovers"))),
      PortionSizeMethod("drink-scale", "No description", "portion/placeholder.jpg", false, Seq(PortionSizeMethodParameter("drinkware-id", "glasses_beer"), PortionSizeMethodParameter("initial-fill-level", "0.9"), PortionSizeMethodParameter("skip-fill-level", "true"))))

    val expected1 = Right(UserFoodData("F003", "Inheritance test 1", Map(), 0, expectedPsm1, true, false, 3456))

    // Must inherit indirectly from C001, but override same as before

    val expected2 = Right(UserFoodData("F006", "Inheritance test 2", Map(), 0, expectedPsm1, true, true, 3456))

    assert(service.foodData("F003", defaultLocale) === expected1)
    assert(service.foodData("F006", defaultLocale) === expected2)
  }

  test("Default attribute values") {
    val expected = UserFoodData("F007", "Default attributes test", Map(), 0, Seq(), false, false, 1000)
  }
  
  test("Get as served definition") {
    val expected1 = Right(AsServedSet("as_served_1", "As served 1", Seq(
      AsServedImage("PStApl1.jpg", 51.3),
      AsServedImage("PStApl2.jpg", 66.96),
      AsServedImage("PStApl3.jpg", 87.4),
      AsServedImage("PStApl4.jpg", 114.07),
      AsServedImage("PStApl5.jpg", 148.89),
      AsServedImage("PStApl6.jpg", 194.33),
      AsServedImage("PStApl7.jpg", 253.65))))

    val expected2 = Right(AsServedSet("as_served_1_leftovers", "As served 1 leftovers", Seq(
      AsServedImage("PStApl8.jpg", 5.0),
      AsServedImage("PStApl9.jpg", 7.37),
      AsServedImage("PStApl10.jpg", 10.86),
      AsServedImage("PStApl11.jpg", 16.02),
      AsServedImage("PStApl12.jpg", 23.61),
      AsServedImage("PStApl13.jpg", 34.8),
      AsServedImage("PStApl14.jpg", 51.3))))

    assert(service.asServedDef("as_served_1") === expected1)

    assert(service.asServedDef("as_served_1_leftovers") === expected2)
  }

  test("Get guide image definition") {
    val expected1 = Right(GuideImage("guide_1", "Guide 1", Seq(
      GuideImageWeightRecord("Aero - Bubbles bag", 1, 135.0),
      GuideImageWeightRecord("Aero - standard Bubbles bag", 2, 39.0),
      GuideImageWeightRecord("Aero - standard bar", 3, 42.5),
      GuideImageWeightRecord("Aero - single", 4, 27.0),
      GuideImageWeightRecord("Aero - medium", 5, 19.0),
      GuideImageWeightRecord("Aero - treatsize", 6, 9.0))))

    val expected2 = Right(GuideImage("guide_2", "Guide 2", Seq(
      GuideImageWeightRecord("1", 1, 1.0),
      GuideImageWeightRecord("2", 2, 2.0),
      GuideImageWeightRecord("3", 3, 3.0),
      GuideImageWeightRecord("4", 4, 4.0))))

    assert(service.guideDef("guide_1") === expected1)
    assert(service.guideDef("guide_2") === expected2)
  }

  test("Get drinkware definition") {
    val expected =
      Right(DrinkwareSet("mugs", "Mugs", "Gmug",
        Seq(DrinkScale(1, "mugs/mug_a_v2.jpg", "mugs/mug_a_fill_v2.png", 470, 420, 52, 375, VolumeFunction(Seq((0.1, 9.0), (0.2, 42.0), (0.3, 78.2), (0.4, 115.8), (0.5, 158.0), (0.6, 196.4), (0.7, 242.4), (0.8, 289.8), (0.9, 346.0), (1.0, 403.4)))),
          DrinkScale(2, "mugs/mug_b_v2.jpg", "mugs/mug_b_fill_v2.png", 470, 420, 39, 367, VolumeFunction(Seq((0.1, 33.2), (0.2, 69.4), (0.3, 100.4), (0.4, 135.2), (0.5, 165.2), (0.6, 195.8), (0.7, 224.8), (0.8, 255.2), (0.9, 285.6), (1.0, 314.6)))),
          DrinkScale(3, "mugs/mug_c_v2.jpg", "mugs/mug_c_fill_v2.png", 470, 420, 52, 341, VolumeFunction(Seq((0.1, 13.4), (0.2, 45.0), (0.3, 81.6), (0.4, 115.0), (0.5, 149.8), (0.6, 182.0), (0.7, 217.2), (0.8, 248.6), (0.9, 284.4), (1.0, 314.6)))),
          DrinkScale(4, "mugs/mug_d_v2.jpg", "mugs/mug_d_fill_v2.png", 470, 420, 112, 291, VolumeFunction(Seq((0.1, 0.0), (0.2, 8.6), (0.3, 31.2), (0.4, 54.2), (0.5, 81.6), (0.6, 107.0), (0.7, 130.4), (0.8, 152.4), (0.9, 180.2), (1.0, 215.4)))),
          DrinkScale(5, "mugs/mug_e_v2.jpg", "mugs/mug_e_fill_v2.png", 470, 420, 53, 283, VolumeFunction(Seq((0.1, 0.0), (0.2, 31.6), (0.3, 52.2), (0.4, 78.6), (0.5, 112.6), (0.6, 137.6), (0.7, 168.0), (0.8, 196.8), (0.9, 225.8), (1.0, 253.0)))),
          DrinkScale(6, "mugs/mug_f_v2.jpg", "mugs/mug_f_fill_v2.png", 470, 420, 76, 354, VolumeFunction(Seq((0.1, 1.0), (0.2, 20.6), (0.3, 58.6), (0.4, 103.4), (0.5, 148.2), (0.6, 202.0), (0.7, 249.2), (0.8, 301.0), (0.9, 348.8), (1.0, 399.8)))))))

    assert(service.drinkwareDef("mugs") === expected)
  }

  test("Get associated food prompts") {
    val expected = Seq(AssociatedFood("C000", "Prompt 1", false, "name1"), AssociatedFood("C001", "Prompt 2", true, "name2"))

    assert(service.associatedFoods("F000", defaultLocale).right.get === expected)
  }

  test("Get brand names") {
    val expected1 = Seq("brand1", "brand2")
    val expected2 = Seq("brand3", "brand4")

    assert(service.brandNames("F000", defaultLocale) === expected1)
    assert(service.brandNames("F001", defaultLocale) === expected2)
  }

}