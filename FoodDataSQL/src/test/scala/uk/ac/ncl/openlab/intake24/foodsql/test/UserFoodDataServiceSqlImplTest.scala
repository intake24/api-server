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

package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.{UserCategoryContents, UserCategoryHeader, UserFoodData, UserFoodHeader}

class UserFoodDataServiceSqlImplTest extends FunSuite with TestFoodDatabase {

  val service = new FoodDatabaseUserImpl(testDataSource)
  
  val defaultVersion = java.util.UUID.fromString("454a02a5-785e-4ca8-af52-81b11c28f56e")

  val en_GB_c0 = UserCategoryHeader("C000", "Category 1")
  val en_GB_c1 = UserCategoryHeader("C001", "Category 2")
  val en_GB_c2 = UserCategoryHeader("C002", "Category 3")
  val en_GB_c3 = UserCategoryHeader("C003", "Category 4")
  val en_GB_c4 = UserCategoryHeader("C004", "Nested category 1")
  val en_GB_c5 = UserCategoryHeader("C005", "Nested category 2")
  val en_GB_c6 = UserCategoryHeader("C006", "Nested category 3")
  
  val test1_c0 = UserCategoryHeader("C000", "Locale 1 Category 1")
  val test1_c1 = UserCategoryHeader("C001", "Locale 1 Category 2")
  val test1_c2 = UserCategoryHeader("C002", "Locale 1 Category 3")
  val test1_c3 = UserCategoryHeader("C003", "Locale 1 Category 4")
  val test1_c4 = UserCategoryHeader("C004", "Locale 1 Nested category 1")
  val test1_c5 = UserCategoryHeader("C005", "Locale 1 Nested category 2")
  val test1_c6 = UserCategoryHeader("C006", "Locale 1 Nested category 3")
  
  val en_GB_f0 = UserFoodHeader("F000", "Food definition test 1")
  val en_GB_f1 = UserFoodHeader("F001", "Uncategorised food")
  val en_GB_f2 = UserFoodHeader("F002", "Parent test")
  val en_GB_f3 = UserFoodHeader("F003", "Inheritance test 1")
  val en_GB_f4 = UserFoodHeader("F004", "Food definition test 2")
  val en_GB_f5 = UserFoodHeader("F005", "Food definition test 3")
  val en_GB_f6 = UserFoodHeader("F006", "Inheritance test 2")
  val en_GB_f7 = UserFoodHeader("F007", "Default attributes test")
  val en_GB_f8 = UserFoodHeader("F008", "PSM test 1")
  
  /*
   * Test data structure:
   * 
   *  C000 (do not use in test1)
   *    F004
   *    F000
   *    F005
   *  C001
   *    C005 (restricted to en_GB, test1)
   *      F002 (do not use in test1)
   *      F006 (do not use in test1)
   *    F004
   *  C002 (hidden)
   *    C006
   *    F002
   *    F007
   *    F008
   *  C003
   *    C004
   *      C005
   *        F002
   *        F006
   */

  test("Get root categories for en_GB") {
    // C002 is hidden
    // C006 is only contained in hidden categories and must be root (unless itself is hidden)
    val expected = Seq(en_GB_c0, en_GB_c1, en_GB_c3, en_GB_c6)

    assert(service.getRootCategories("en_GB") === expected)
  }
  
  test("Get root categories for test1") {
    // C002 is hidden
    // C000 is marked as "do not use" for locale test1
    // C006 is only contained in hidden categories and must be root (unless itself is hidden)    
    val expected = Seq(test1_c1, test1_c3, test1_c6)
  }
  
  test("Restricted category should be treated as missing") { 
    assert(service.getCategoryContents("C005", "test2") === Left(RecordNotFound))
  }
      
  test("Category contents") {
    assert(service.getCategoryContents("C005", "en_GB") === Right(UserCategoryContents(Seq(/* alphabetical order */ en_GB_f6, en_GB_f2), Seq())))    
  }
  
  test("Empty category due to restrictions") {
    assert(service.getCategoryContents("C005", "test1") === Right(UserCategoryContents(Seq(), Seq())))
  }  
  
  test("Default attribute values") {
    val expected = Right(UserFoodData("F007", "Default attributes test", Map(), 0, Seq(), false, false, 1000))
    
    assert(service.getFoodData("F007", "en_GB") === expected)
  }
}
