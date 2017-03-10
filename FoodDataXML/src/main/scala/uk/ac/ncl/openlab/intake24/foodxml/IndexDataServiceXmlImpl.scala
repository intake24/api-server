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

package uk.ac.ncl.openlab.intake24.foodxml

import org.slf4j.LoggerFactory

import com.google.inject.Inject
import com.google.inject.Singleton


import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndexDataService

@Singleton
class FoodIndexDataServiceXmlImpl @Inject() (data: XmlDataSource) extends FoodIndexDataService {

  val defaultLocale = "en_GB"
  
  import Util._

  val log = LoggerFactory.getLogger(classOf[FoodIndexDataServiceXmlImpl])

  def indexableCategories(locale: String) = {
    checkLocale(locale)
    Right(data.categories.categories.filterNot(_.isHidden).map(mkHeader))
  }

  def indexableFoods(locale: String) = {
    checkLocale(locale)
    Right(data.foods.foods.map(mkHeader))
  }

  def splitList(locale: String) = {
    checkLocale(locale)
    Right(data.split)
  }

  def synsets(locale: String) = {
    checkLocale(locale)
    Right(data.synSets)
  }

}