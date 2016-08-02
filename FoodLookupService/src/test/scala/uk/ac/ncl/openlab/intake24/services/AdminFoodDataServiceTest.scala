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
import uk.ac.ncl.openlab.intake24.CategoryHeader

import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter

import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.GuideImageWeightRecord
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.DrinkScale
import uk.ac.ncl.openlab.intake24.VolumeFunction

import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.UserFoodData
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord

abstract class AdminFoodDataServiceTest extends FunSuite {

  val service: AdminFoodDataService

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
      FoodRecord(MainFoodRecord(defaultVersion, "F000", "Food definition test 1", 1, InheritableAttributes(Some(true), None, None)),
        LocalFoodRecord(Some(defaultVersion), Some("Food definition test 1"), false, Map("NDNS" -> "100"),
          Seq(PortionSizeMethod("as-served", "Test", "portion/placeholder.jpg", false,
            Seq(PortionSizeMethodParameter("serving-image-set", "as_served_1"), PortionSizeMethodParameter("leftovers-image-set", "as_served_1_leftovers"))))))
    val expected2 =
      FoodRecord(MainFoodRecord(defaultVersion, "F004", "Food definition test 2", 10, InheritableAttributes(None, Some(true), None)),
        LocalFoodRecord(Some(defaultVersion), Some("Food definition test 2"), false, Map("NDNS" -> "200"), Seq()))
    val expected3 =
      FoodRecord(MainFoodRecord(defaultVersion, "F005", "Food definition test 3", 20, InheritableAttributes(None, None, Some(1234))),
        LocalFoodRecord(None, Some("Food definition test 3"), false, Map("NDNS" -> "300"), Seq()))

    // This hack is required because version codes are generated randomly on import
        
    val actual1 = service.foodRecord("F000", defaultLocale).right.get
    val versionOverride1 = actual1.copy(main = actual1.main.copy(version = defaultVersion), local = actual1.local.copy(version = Some(defaultVersion)))
        
    val actual2 = service.foodRecord("F004", defaultLocale).right.get
    val versionOverride2 = actual2.copy(main = actual2.main.copy(version = defaultVersion), local = actual2.local.copy(version = Some(defaultVersion)))
    
    val actual3 = service.foodRecord("F005", defaultLocale).right.get
    val versionOverride3 = actual3.copy(main = actual3.main.copy(version = defaultVersion), local = actual3.local.copy(version = None))
    
    assert( versionOverride1 === expected1)
    assert( versionOverride2 === expected2)
    assert( versionOverride3 === expected3)
  }

  test("Default attribute values") {
    val expected = UserFoodData("F007", "Default attributes test", Map(), 0, Seq(), false, false, 1000)
  }

  test("Portion size methods in food definition") {
    val portionSizeMethods = Seq(
      PortionSizeMethod("as-served", "Blah", "portion/placeholder.jpg", false, Seq(PortionSizeMethodParameter("serving-image-set", "as_served_1"), PortionSizeMethodParameter("leftovers-image-set", "as_served_1_leftovers"))),
      PortionSizeMethod("guide-image", "Blah Blah", "test.jpg", true, Seq(PortionSizeMethodParameter("guide-image-id", "guide1"))))

    // check that definition is correct
    val expected1 =
      FoodRecord(
        MainFoodRecord(defaultVersion, "F008", "PSM test 1", 0, InheritableAttributes(None, None, None)),
        LocalFoodRecord(Some(defaultVersion), Some("PSM test 1"), false, Map(), portionSizeMethods))

    // then check that uninherited PSM data is returned correctly
    // val expected2 = FoodData("F008", "PSM test 1", Map(), 0, portionSizeMethods, false, false, 1000)

    val actual1 = service.foodRecord("F008", defaultLocale).right.get
    val versionOverride1 = actual1.copy(main = actual1.main.copy(version = defaultVersion), local = actual1.local.copy(version = Some(defaultVersion)))
    
    assert(versionOverride1 === expected1)
    //assert(service.foodData("F008", defaultLocale) === expected2)
  }
}
