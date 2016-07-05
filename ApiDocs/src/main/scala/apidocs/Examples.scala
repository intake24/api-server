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


import uk.ac.ncl.openlab.intake24.Category
import uk.ac.ncl.openlab.intake24.CategoryLocal
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import java.io.StringReader
import upickle.default._
import java.util.UUID
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryContents
import uk.ac.ncl.openlab.intake24.api.Intake24Credentials
import uk.ac.ncl.openlab.intake24.services.NewFood

import uk.ac.ncl.openlab.intake24.services.NewCategory
import uk.ac.ncl.openlab.intake24.CategoryBase
import uk.ac.ncl.openlab.intake24.FoodGroup

import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.FoodRecord

object Examples {

  val xmlVersionId = java.util.UUID.fromString("454a02a5-785e-4ca8-af52-81b11c28f56e")

  val gsonBuilder = new GsonBuilder().setPrettyPrinting().create()
  val gsonParser = new JsonParser()

  def prettyPrint(json: String) = {
    val je = gsonParser.parse(json)
    gsonBuilder.toJson(je)
  }

  val signInRequest = Intake24Credentials("", "admin", "admin_password")

  val signInRequestJson = prettyPrint(write(signInRequest))

  val food1 = FoodRecord(xmlVersionId, "MLAS", "Meat lasagne (includes homemade)", 107, InheritableAttributes(Some(true), None, None),
    LocalFoodRecord(Some(xmlVersionId), Some("Лазанья с мясом (в том числе домашнего приготовления)"), false, Map("NDNS" -> "1348"), Seq(PortionSizeMethod("as-served", "Use an image", "portion/lasagne.jpg", false,
      Seq(PortionSizeMethodParameter("serving-image-set", "lasagne"),
        PortionSizeMethodParameter("leftovers-image-set", "lasagne_leftovers"))))))

  val food1Json = prettyPrint(write(food1))

  val food1AttrJson = prettyPrint(write(food1.attributes))

  val food1PsmJson = prettyPrint(write(food1.localData.portionSize(0)))

  val cat = Category(xmlVersionId, "BRED", "Bread/crumpets/pancakes/yorkshire puddings", false, InheritableAttributes(None, None, None),
    CategoryLocal(Some(xmlVersionId), Some("Хлебобулочные изделия/блины/пудинги"), Seq()))

  val cat2 = Category(xmlVersionId, "BSCT", "Biscuits & crackers", false, InheritableAttributes(None, None, None),
    CategoryLocal(Some(xmlVersionId), Some("Печенье и крекеры"), Seq()))

  val catJson = prettyPrint(write(cat))

  val catHeader = CategoryHeader(cat.code, cat.englishDescription, cat.localData.localDescription, cat.isHidden)

  val catHeaderJson = prettyPrint(write(catHeader))

  val foodHeader = FoodHeader(food1.code, food1.englishDescription, food1.localData.localDescription, false)

  val foodHeaderJson = prettyPrint(write(foodHeader))

  val catContents = CategoryContents(Seq(foodHeader), Seq(catHeader))

  val catContentsJson = prettyPrint(write(catContents))

  val rootCats = prettyPrint(write(Seq(cat, cat2)))

  val token = prettyPrint("""{ "token" : "(URL-encoded JSON Web Token)"}""")

  val invalidRequest = prettyPrint(""" { "error" : "(error type)", "error_code" : "(error code)", "message" : "(debug message)" } """)

  val newFoodRequest = prettyPrint(write(NewFood("F001", "New food", 1, InheritableAttributes(Some(true), None, None))))
  
  val newCategoryRequest = prettyPrint(write(NewCategory("C001", "New category", false, InheritableAttributes(Some(true), None, None))))
  
  val foodBase = prettyPrint(write(MainFoodRecord(xmlVersionId, "F001", "Updated food", 1, InheritableAttributes(Some(true), None, None))))
  
  val foodLocal = prettyPrint(write(LocalFoodRecord(Some(xmlVersionId), Some("Лазанья с мясом (в том числе домашнего приготовления)"), false, Map("NDNS" -> "1348"), Seq(PortionSizeMethod("as-served", "Use an image", "portion/lasagne.jpg", false,
      Seq(PortionSizeMethodParameter("serving-image-set", "lasagne"),
        PortionSizeMethodParameter("leftovers-image-set", "lasagne_leftovers")))))))
        
  val categoryBase = prettyPrint(write(CategoryBase(xmlVersionId, "C001", "Updated category", true, InheritableAttributes(Some(true), None, None))))
        
  val categoryLocal = prettyPrint(write(CategoryLocal(Some(xmlVersionId), Some("Хлебобулочные изделия/блины/пудинги"),  Seq(PortionSizeMethod("as-served", "Use an image", "portion/lasagne.jpg", false,
      Seq(PortionSizeMethodParameter("serving-image-set", "lasagne"),
        PortionSizeMethodParameter("leftovers-image-set", "lasagne_leftovers")))))))
        
  val foodGroup = prettyPrint(write(FoodGroup(1, "White bread/rolls", Some("Белый хлеб"))))
}