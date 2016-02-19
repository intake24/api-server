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
import java.sql.DriverManager
import anorm.SQL
import uk.ac.ncl.openlab.intake24.foodsql.tools.XmlImporter
import uk.ac.ncl.openlab.intake24.foodsql.AdminFoodDataServiceSqlImpl
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import net.scran24.fooddef.Food
import net.scran24.fooddef.InheritableAttributes
import net.scran24.fooddef.PortionSizeMethod
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.AsServedImage
import net.scran24.fooddef.VolumeFunction
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.DrinkScale
import net.scran24.fooddef.Prompt
import net.scran24.fooddef.GuideImageWeightRecord
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.FoodData
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import net.scran24.fooddef.PortionSizeMethodParameter
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataServiceTest
import uk.ac.ncl.openlab.intake24.foodsql.UserFoodDataServiceSqlImpl
import net.scran24.fooddef.UserCategoryHeader

class UserFoodDataServiceSqlImplTest extends FunSuite with TestDB {

  val hikariConfig = new HikariConfig()

  hikariConfig.setJdbcUrl(s"jdbc:postgresql://localhost/intake24_foods_test")
  hikariConfig.setDriverClassName("org.postgresql.Driver")
  hikariConfig.setUsername("postgres")

  val dataSource = new HikariDataSource(hikariConfig)
  
  val testDataStatements = loadStatementsFromResource("/sql/test/user_test_data.sql")
  
  testDataStatements.foreach {
    stmt =>
      logger.debug(stmt)
      SQL(stmt).execute()
  }

  val service = new UserFoodDataServiceSqlImpl(dataSource)
  
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
  
  /*
   * Test data structure:
   * 
   *  C000 (do not use in test1)
   *    F004
   *    F000
   *    F005
   *  C001
   *    C005
   *      F002
   *      F006
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

    assert(service.rootCategories("en_GB") === expected)
  }
  
  test("Get root categories for test1") {
    // C002 is hidden
    // C000 is marked as "do not use" for locale test1
    // C006 is only contained in hidden categories and must be root (unless itself is hidden)    
    val expected = Seq(test1_c1, test1_c3, test1_c6)
  }
    
/*
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

  test("Default attribute values") {
    val expected = FoodData("F007", "Default attributes test", Map(), 0, Seq(), false, false, 1000)
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
    // val expected2 = FoodData("F008", "PSM test 1", Map(), 0, portionSizeMethods, false, false, 1000)

    val actual1 = service.foodDef("F008", defaultLocale)
    val versionOverride1 = actual1.copy(version = defaultVersion, localData = actual1.localData.copy(version = Some(defaultVersion)))
    
    assert(versionOverride1 === expected1)
    //assert(service.foodData("F008", defaultLocale) === expected2)
  }*/
}
