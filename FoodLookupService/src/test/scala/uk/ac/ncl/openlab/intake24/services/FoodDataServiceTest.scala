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
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.Food
import net.scran24.fooddef.InheritableAttributes
import net.scran24.fooddef.PortionSizeMethod
import net.scran24.fooddef.PortionSizeMethodParameter
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.AsServedImage
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.GuideImageWeightRecord
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.DrinkScale
import net.scran24.fooddef.VolumeFunction
import net.scran24.fooddef.Prompt
import net.scran24.fooddef.FoodLocal

abstract class FoodDataServiceTest extends FunSuite {

  val service: FoodDataService

  val defaultLocale = "en_GB"

  val defaultVersion = java.util.UUID.fromString("454a02a5-785e-4ca8-af52-81b11c28f56e")

  val c0 = CategoryHeader("C000", "Category 1", Some("Category 1"), false)
  val c1 = CategoryHeader("C001", "Category 2", Some("Category 2"), false)
  val c2 = CategoryHeader("C002", "Category 3", Some("Category 3"), true)
  val c3 = CategoryHeader("C003", "Category 4", Some("Category 4"), false)
  val c4 = CategoryHeader("C004", "Nested category 1", Some("Nested category 1"), false)
  val c5 = CategoryHeader("C005", "Nested category 2", Some("Nested category 2"), false)

  test("Get root categories") {
    val expected = Seq(CategoryHeader("C000", "Category 1", Some("Category 1"), false), CategoryHeader("C001", "Category 2", Some("Category 2"), false),
      CategoryHeader("C002", "Category 3", Some("Category 3"), true), CategoryHeader("C003", "Category 4", Some("Category 4"), false))

    assert(service.rootCategories(defaultLocale) === expected)
  }

  test("Get direct parent categories for uncategorised food") {
    val expected = Seq[CategoryHeader]()

    assert(service.foodParentCategories("F001", defaultLocale) === expected)
  }

  test("Get all parent categories for uncategorised food") {
    val expected = Seq[CategoryHeader]()

    assert(service.foodAllCategories("F001", defaultLocale) === expected)
  }

  test("Get direct parent categories for food") {
    val expected = Seq(c2, c5)

    assert(service.foodParentCategories("F002", defaultLocale) === expected)
  }

  test("Get all parent categories for food") {
    val expected = Seq(c2, c5, c1, c4, c3)

    assert(service.foodAllCategories("F002", defaultLocale) === expected)
  }

  test("Get direct parent categories for root category") {
    val expected = Seq[CategoryHeader]()

    assert(service.categoryParentCategories("C000", defaultLocale) === expected)
  }

  test("Get all parent categories for root category") {
    val expected = Seq[CategoryHeader]()

    assert(service.categoryAllCategories("C000", defaultLocale) === expected)
  }

  test("Get direct parent categories for nested category") {
    val expected = Seq(c1, c4)

    assert(service.categoryParentCategories("C005", defaultLocale) === expected)
  }

  test("Get all parent categories for nested category") {
    val expected = Seq(c1, c4, c3)

    assert(service.categoryAllCategories("C005", defaultLocale) === expected)
  }

  test("Get food definition") {
    val expected1 =
      Food(defaultVersion, "F000", "Food definition test 1", 1, InheritableAttributes(Some(true), None, None),
        FoodLocal(Some(defaultVersion), Some("Food definition test 1"), Map("NDNS" -> "100"),
          Seq(PortionSizeMethod("as-served", "Test", "portion/placeholder.jpg", false,
            Seq(PortionSizeMethodParameter("serving-image-set", "as_served_1"), PortionSizeMethodParameter("leftovers-image-set", "as_served_1_leftovers"))))))
    val expected2 =
      Food(defaultVersion, "F004", "Food definition test 2", 10, InheritableAttributes(None, Some(true), None),
        FoodLocal(Some(defaultVersion), Some("Food definition test 2"), Map("NDNS" -> "200"), Seq()))
    val expected3 =
      Food(defaultVersion, "F005", "Food definition test 3", 20, InheritableAttributes(None, None, Some(1234)),
        FoodLocal(None, Some("Food definition test 3"), Map("NDNS" -> "300"), Seq()))

    // This hack is required because version codes are generated randomly on import
        
    val actual1 = service.foodDef("F000", defaultLocale)
    val versionOverride1 = actual1.copy(version = defaultVersion, localData = actual1.localData.copy(version = Some(defaultVersion)))
        
    val actual2 = service.foodDef("F004", defaultLocale)
    val versionOverride2 = actual2.copy(version = defaultVersion, localData = actual2.localData.copy(version = Some(defaultVersion)))
    
    val actual3 = service.foodDef("F005", defaultLocale)
    val versionOverride3 = actual3.copy(version = defaultVersion, localData = actual3.localData.copy(version = None))
    
    assert( versionOverride1 === expected1)
    assert( versionOverride2 === expected2)
    assert( versionOverride3 === expected3)
  }

  test("Attribute & portion size method inheritance") {
    // Must inherit directly from C001
    val expectedPsm1 = Seq(
      PortionSizeMethod("as-served", "No description", "portion/placeholder.jpg", false, Seq(PortionSizeMethodParameter("serving-image-set", "as_served_2"), PortionSizeMethodParameter("leftovers-image-set", "as_served_2_leftovers"))),
      PortionSizeMethod("drink-scale", "No description", "portion/placeholder.jpg", false, Seq(PortionSizeMethodParameter("drinkware-id", "glasses_beer"), PortionSizeMethodParameter("initial-fill-level", "0.9"), PortionSizeMethodParameter("skip-fill-level", "true"))))

    val expected1 = FoodData("F003", "Inheritance test 1", Some("Inheritance test 1"), Map(), 0, expectedPsm1, true, false, 3456)

    // Must inherit indirectly from C001, but override same as before

    val expected2 = FoodData("F006", "Inheritance test 2", Some("Inheritance test 2"), Map(), 0, expectedPsm1, true, true, 3456)

    assert(service.foodData("F003", defaultLocale) === expected1)
    assert(service.foodData("F006", defaultLocale) === expected2)
  }

  test("Default attribute values") {
    val expected = FoodData("F007", "Default attributes test", Some("Default attributes test"), Map(), 0, Seq(), false, false, 1000)
  }

  test("Portion size methods in food definition") {
    val portionSizeMethods = Seq(
      PortionSizeMethod("as-served", "Blah", "portion/placeholder.jpg", false, Seq(PortionSizeMethodParameter("serving-image-set", "as_served_1"), PortionSizeMethodParameter("leftovers-image-set", "as_served_1_leftovers"))),
      PortionSizeMethod("guide-image", "Blah Blah", "test.jpg", true, Seq(PortionSizeMethodParameter("guide-image-id", "guide1"))))

    // check that definition is correct
    val expected1 =
      Food(
        defaultVersion, "F008", "PSM test 1", 0, InheritableAttributes(None, None, None),
        FoodLocal(Some(defaultVersion), Some("PSM test 1"), Map(), portionSizeMethods))

    // then check that uninherited PSM data is returned correctly
    val expected2 = FoodData("F008", "PSM test 1", Some("PSM test 1"), Map(), 0, portionSizeMethods, false, false, 1000)

    val actual1 = service.foodDef("F008", defaultLocale)
    val versionOverride1 = actual1.copy(version = defaultVersion, localData = actual1.localData.copy(version = Some(defaultVersion)))
    
    assert(versionOverride1 === expected1)
    assert(service.foodData("F008", defaultLocale) === expected2)
  }

  test("Get as served definition") {
    val expected1 = AsServedSet("as_served_1", "As served 1", Seq(
      AsServedImage("PStApl1.jpg", 51.3),
      AsServedImage("PStApl2.jpg", 66.96),
      AsServedImage("PStApl3.jpg", 87.4),
      AsServedImage("PStApl4.jpg", 114.07),
      AsServedImage("PStApl5.jpg", 148.89),
      AsServedImage("PStApl6.jpg", 194.33),
      AsServedImage("PStApl7.jpg", 253.65)))

    val expected2 = AsServedSet("as_served_1_leftovers", "As served 1 leftovers", Seq(
      AsServedImage("PStApl8.jpg", 5.0),
      AsServedImage("PStApl9.jpg", 7.37),
      AsServedImage("PStApl10.jpg", 10.86),
      AsServedImage("PStApl11.jpg", 16.02),
      AsServedImage("PStApl12.jpg", 23.61),
      AsServedImage("PStApl13.jpg", 34.8),
      AsServedImage("PStApl14.jpg", 51.3)))

    assert(service.asServedDef("as_served_1") === expected1)

    assert(service.asServedDef("as_served_1_leftovers") === expected2)
  }

  test("Get guide image definition") {
    val expected1 = GuideImage("guide_1", "Guide 1", Seq(
      GuideImageWeightRecord("Aero - Bubbles bag", 1, 135.0),
      GuideImageWeightRecord("Aero - standard Bubbles bag", 2, 39.0),
      GuideImageWeightRecord("Aero - standard bar", 3, 42.5),
      GuideImageWeightRecord("Aero - single", 4, 27.0),
      GuideImageWeightRecord("Aero - medium", 5, 19.0),
      GuideImageWeightRecord("Aero - treatsize", 6, 9.0)))

    val expected2 = GuideImage("guide_2", "Guide 2", Seq(
      GuideImageWeightRecord("1", 1, 1.0),
      GuideImageWeightRecord("2", 2, 2.0),
      GuideImageWeightRecord("3", 3, 3.0),
      GuideImageWeightRecord("4", 4, 4.0)))

    assert(service.guideDef("guide_1") === expected1)
    assert(service.guideDef("guide_2") === expected2)
  }

  test("Get drinkware definition") {
    val expected =
      DrinkwareSet("mugs", "Mugs", "Gmug",
        Seq(DrinkScale(1, "mugs/mug_a_v2.jpg", "mugs/mug_a_fill_v2.png", 470, 420, 52, 375, VolumeFunction(Seq((0.1, 9.0), (0.2, 42.0), (0.3, 78.2), (0.4, 115.8), (0.5, 158.0), (0.6, 196.4), (0.7, 242.4), (0.8, 289.8), (0.9, 346.0), (1.0, 403.4)))),
          DrinkScale(2, "mugs/mug_b_v2.jpg", "mugs/mug_b_fill_v2.png", 470, 420, 39, 367, VolumeFunction(Seq((0.1, 33.2), (0.2, 69.4), (0.3, 100.4), (0.4, 135.2), (0.5, 165.2), (0.6, 195.8), (0.7, 224.8), (0.8, 255.2), (0.9, 285.6), (1.0, 314.6)))),
          DrinkScale(3, "mugs/mug_c_v2.jpg", "mugs/mug_c_fill_v2.png", 470, 420, 52, 341, VolumeFunction(Seq((0.1, 13.4), (0.2, 45.0), (0.3, 81.6), (0.4, 115.0), (0.5, 149.8), (0.6, 182.0), (0.7, 217.2), (0.8, 248.6), (0.9, 284.4), (1.0, 314.6)))),
          DrinkScale(4, "mugs/mug_d_v2.jpg", "mugs/mug_d_fill_v2.png", 470, 420, 112, 291, VolumeFunction(Seq((0.1, 0.0), (0.2, 8.6), (0.3, 31.2), (0.4, 54.2), (0.5, 81.6), (0.6, 107.0), (0.7, 130.4), (0.8, 152.4), (0.9, 180.2), (1.0, 215.4)))),
          DrinkScale(5, "mugs/mug_e_v2.jpg", "mugs/mug_e_fill_v2.png", 470, 420, 53, 283, VolumeFunction(Seq((0.1, 0.0), (0.2, 31.6), (0.3, 52.2), (0.4, 78.6), (0.5, 112.6), (0.6, 137.6), (0.7, 168.0), (0.8, 196.8), (0.9, 225.8), (1.0, 253.0)))),
          DrinkScale(6, "mugs/mug_f_v2.jpg", "mugs/mug_f_fill_v2.png", 470, 420, 76, 354, VolumeFunction(Seq((0.1, 1.0), (0.2, 20.6), (0.3, 58.6), (0.4, 103.4), (0.5, 148.2), (0.6, 202.0), (0.7, 249.2), (0.8, 301.0), (0.9, 348.8), (1.0, 399.8))))))

    assert(service.drinkwareDef("mugs") == expected)
  }

  test("Get associated food prompts") {
    val expected = Seq(Prompt("C000", "Prompt 1", false, "name1"), Prompt("C001", "Prompt 2", true, "name2"))

    assert(service.associatedFoodPrompts("F000", defaultLocale) === expected)
  }

  test("Get brand names") {
    val expected1 = Seq("brand1", "brand2")
    val expected2 = Seq("brand3", "brand4")

    assert(service.brandNames("F000", defaultLocale) === expected1)
    assert(service.brandNames("F001", defaultLocale) === expected2)
  }

}