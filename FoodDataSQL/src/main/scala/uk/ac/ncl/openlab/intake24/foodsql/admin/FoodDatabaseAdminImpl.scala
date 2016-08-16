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

package uk.ac.ncl.openlab.intake24.foodsql.admin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService

@Singleton
class FoodDatabaseAdminImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends FoodDatabaseAdminService      
    with FoodsAdminImpl
    with CategoriesAdminImpl       
    with FoodBrowsingAdminImpl
    with QuickSearchAdminImpl
    with FoodGroupsAdminImpl
    with GuideImageAdminImpl
    with AsServedImageAdminImpl
    with DrinkwareAdminImpl
    with HeaderRows
    with NutrientTablesAdminImpl    
    with AssociatedFoodsAdminImpl
    with BrandNamesAdminImpl 
    with FoodIndexDataAdminImpl { }


/* As seen from class FoodDatabaseAdminImpl, the missing signatures are as follows.
 *  For convenience, these are usable as stub implementations.
 
  // Members declared in uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
  def createLocalCategories(localCategoryRecords: Map[String,uk.ac.ncl.openlab.intake24.LocalCategoryRecord]): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError,Unit] = ???
  def updateCategoryLocalRecord(categoryCode: String,locale: String,categoryLocal: uk.ac.ncl.openlab.intake24.LocalCategoryRecord): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError,Unit] = ???
  def updateCategoryMainRecord(categoryCode: String,categoryMain: uk.ac.ncl.openlab.intake24.MainCategoryRecord): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError,Unit] = ???
  
  // Members declared in uk.ac.ncl.openlab.intake24.services.fooddb.admin.DrinkwareAdminService
  def allDrinkwareSets(): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError,Seq[uk.ac.ncl.openlab.intake24.DrinkwareHeader]] = ???
  
  // Members declared in uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService
  def drinkwareSet(id: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError,uk.ac.ncl.openlab.intake24.DrinkwareSet] = ???
  
  // Members declared in uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
  def categoryAllCategoriesCodes(code: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.CategoryCodeError,Seq[String]] = ???
  def cateogryAllCategoriesHeaders(code: String,locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.CategoryCodeError,Seq[uk.ac.ncl.openlab.intake24.CategoryHeader]] = ???
  def foodAllCategoriesCodes(code: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.CategoryCodeError,Seq[String]] = ???
  def foodAllCategoriesHeaders(code: String,locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalFoodCodeError,Seq[uk.ac.ncl.openlab.intake24.CategoryHeader]] = ???
  
  // Members declared in uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodIndexDataAdminService
  def createSplitList(splitList: uk.ac.ncl.openlab.intake24.SplitList,locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError,Unit] = ???
  def createSynsets(synsets: Seq[Set[String]],locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError,Unit] = ???
  def deleteSplitList(locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError,Unit] = ???
  def deleteSynsets(locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError,Unit] = ???
  def splitList(locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError,uk.ac.ncl.openlab.intake24.SplitList] = ???
  def synsets(locale: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError,Seq[Set[String]]] = ???
  
  // Members declared in uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
  def createLocalFoods(localFoodRecords: Map[String,uk.ac.ncl.openlab.intake24.LocalFoodRecord]): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError,Unit] = ???
  
  // Members declared in uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
  def guideImage(id: String): Either[uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError,uk.ac.ncl.openlab.intake24.GuideImage] = ???
*/