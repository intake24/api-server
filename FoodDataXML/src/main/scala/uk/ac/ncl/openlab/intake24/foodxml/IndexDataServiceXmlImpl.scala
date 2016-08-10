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

import uk.ac.ncl.openlab.intake24.SplitList
import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.UserFoodHeader

import uk.ac.ncl.openlab.intake24.services.foodindex.Util.mkHeader
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndexDataService

@Singleton
class FoodIndexDataServiceXmlImpl @Inject() (data: XmlDataSource) extends FoodIndexDataService {

  val defaultLocale = "en_GB"

  val log = LoggerFactory.getLogger(classOf[FoodIndexDataServiceXmlImpl])

  def checkLocale(locale: String) = if (locale != defaultLocale)
    log.warn("Locales other than en_GB are not supported by this implementation -- returning en_GB results for debug purposes");

  def indexableCategories(locale: String): Seq[UserCategoryHeader] = {
    checkLocale(locale)
    data.categories.categories.filterNot(_.isHidden).map(mkHeader)
  }

  def indexableFoods(locale: String): Seq[UserFoodHeader] = {
    checkLocale(locale)
    data.foods.foods.map(mkHeader)
  }

  def splitList(locale: String): SplitList = {
    checkLocale(locale)
    data.split
  }

  def synsets(locale: String): Seq[Set[String]] = {
    checkLocale(locale)
    data.synSets
  }

}