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
import scala.xml.XML
import net.scran24.fooddef.Food
import net.scran24.fooddef.InheritableAttributes
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.Prompt
import java.io.File
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.Category
import net.scran24.fooddef.FoodHeader
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.CategoryContents
import com.google.inject.Singleton
import com.google.inject.Inject
import com.google.inject.BindingAnnotation
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.FoodDataService
import uk.ac.ncl.openlab.intake24.services.LookupServiceUtil
import uk.ac.ncl.openlab.intake24.services.foodindex.Util._
import java.util.UUID
import net.scran24.fooddef.CategoryLocal
import net.scran24.fooddef.AsServedHeader
import net.scran24.fooddef.GuideHeader
import net.scran24.fooddef.DrinkwareHeader
import net.scran24.fooddef.NutrientTable

case class FoodDataServiceXmlConfiguration(
  baseDirPath: String,
  foodsFilePath: String,
  foodGroupsFilePath: String,
  brandsFilePath: String,
  categoriesFilePath: String,
  asServedFilePath: String,
  drinkwareFilePath: String,
  excludeListPath: String,
  indexFilterPath: String,
  synsetPath: String,
  splitListPath: String,
  guideFilePath: String,
  promptFilePath: String,
  nutrientTablePath: String)

@Singleton
class FoodDataServiceXmlImpl @Inject() (@Named("xml-data-path") dataPath: String) extends FoodDataService {

  import Util._

  val defaultLocale = "en_GB"

  val config = FoodDataServiceXmlConfiguration(
    dataPath,
    dataPath + File.separator + "foods.xml",
    dataPath + File.separator + "food-groups.xml",
    dataPath + File.separator + "brands.xml",
    dataPath + File.separator + "categories.xml",
    dataPath + File.separator + "as-served.xml",
    dataPath + File.separator + "drinkware.xml",
    dataPath + File.separator + "index_exclude",
    dataPath + File.separator + "index_filter",
    dataPath + File.separator + "synsets",
    dataPath + File.separator + "split_list",
    dataPath + File.separator + "guide.xml",
    dataPath + File.separator + "prompts.xml",
    dataPath + File.separator + "nutrients.csv")

  val log = LoggerFactory.getLogger(classOf[FoodDataServiceXmlImpl])

  log.info("Loading data files from " + config.baseDirPath)

  val categories = Categories(CategoryDef.parseXml(XML.load(config.categoriesFilePath)))
  val foods = Foods(FoodDef.parseXml(XML.load(config.foodsFilePath)))
  val foodGroups = FoodGroups(foods.foods, FoodGroupDef.parseXml(XML.load(config.foodGroupsFilePath)))
  val brandNamesMap = BrandDef.parseXml(XML.load(config.brandsFilePath)).withDefaultValue(Seq())
  val inheritance = Inheritance(categories)
  val asServedSets = AsServedDef.parseXml(XML.load(config.asServedFilePath))
  val drinkwareSets = DrinkwareDef.parseXml(XML.load(config.drinkwareFilePath))
  val nonIndexedWords = LookupServiceUtil.readLines(config.excludeListPath)
  val indexFilter = LookupServiceUtil.readLines(config.indexFilterPath)
  val synSets = LookupServiceUtil.readLines(config.synsetPath).map(_.split("\\s+").toSet).toSeq
  val guideImages = GuideImageDef.parseXml(XML.load(config.guideFilePath))
  val prompts = PromptDef.parseXml(XML.load(config.promptFilePath)).withDefaultValue(Seq())
  val split = SplitListDef.parseFile(config.splitListPath)

  def checkLocale(locale: String) = if (locale != defaultLocale)
    log.warn("Locales other than en_GB are not supported by this implementation -- returning en_GB results for debug purposes");

  def allCategories(locale: String): Seq[CategoryHeader] = {
    checkLocale(locale)
    categories.categories.map(mkHeader)
  }

  def allFoods(locale: String): Seq[FoodHeader] = {
    checkLocale(locale)
    foods.foods.map(mkHeader)
  }
  
  def isCategoryCode(code: String): Boolean = categories.categoryMap.contains(code)
  
  def isFoodCode(code: String): Boolean = foods.foodMap.contains(code)

  def uncategorisedFoods(locale: String): Seq[FoodHeader] = {
    checkLocale(locale)
    foods.foods.filter(f => foodParentCategories(f.code, locale).isEmpty).map(mkHeader)
  }

  def rootCategories(locale: String): Seq[CategoryHeader] = {
    checkLocale(locale)
    categories.rootCategories.map(mkHeader)
  }

  def categoryContents(code: String, locale: String): CategoryContents = {
    checkLocale(locale)

    val cat = categories.find(code)

    val foodHeaders = cat.foods.map(fcode => mkHeader(foods.find(fcode)))
    val categoryHeaders = cat.subcategories.map(catcode => mkHeader(categories.find(catcode)))

    CategoryContents(foodHeaders, categoryHeaders)
  }

  def foodDef(code: String, locale: String): Food = {
    checkLocale(locale)
    foods.find(code)
  }

  def foodData(code: String, locale: String): FoodData = {
    checkLocale(locale)

    val f = foods.find(code)

    val portionSizeMethods = {
      val ps = f.localData.portionSize
      if (ps.isEmpty) {
        inheritance.foodInheritedPortionSize(code)
      } else
        ps
    }

    val readyMealOption = f.attributes.readyMealOption match {
      case Some(value) => value
      case None => inheritance.foodInheritedAttribute(code, _.readyMealOption) match {
        case Some(value) => value
        case None => InheritableAttributes.readyMealDefault
      }
    }

    val sameAsBeforeOption = f.attributes.sameAsBeforeOption match {
      case Some(value) => value
      case None => inheritance.foodInheritedAttribute(code, _.sameAsBeforeOption) match {
        case Some(value) => value
        case None => InheritableAttributes.sameAsBeforeDefault
      }
    }

    val reasonableAmount = f.attributes.reasonableAmount match {
      case Some(value) => value
      case None => inheritance.foodInheritedAttribute(code, _.reasonableAmount) match {
        case Some(value) => value
        case None => InheritableAttributes.reasonableAmountDefault
      }
    }

    FoodData(f.code, f.englishDescription, Some(f.englishDescription), f.localData.nutrientTableCodes, f.groupCode, portionSizeMethods, readyMealOption, sameAsBeforeOption, reasonableAmount)
  }

  def foodParentCategories(code: String, locale: String): Seq[CategoryHeader] = {
    checkLocale(locale)
    categories.foodSuperCategories(code).map(code => mkHeader(categories.categoryMap(code)))
  }

  def foodAllCategories(code: String, locale: String): Seq[CategoryHeader] = {
    checkLocale(locale)
    categories.foodAllCategories(code).map(code => mkHeader(categories.categoryMap(code)))
  }

  def categoryParentCategories(code: String, locale: String): Seq[CategoryHeader] = {
    checkLocale(locale)
    categories.categorySuperCategories(code).map(code => mkHeader(categories.categoryMap(code)))
  }

  def categoryAllCategories(code: String, locale: String): Seq[CategoryHeader] = {
    checkLocale(locale)
    categories.categoryAllCategories(code).map(code => mkHeader(categories.categoryMap(code)))
  }

  def categoryDef(code: String, locale: String): Category = {
    checkLocale(locale)
    val cat = categories.find(code)
    Category(cat.version, cat.code, cat.description, cat.isHidden, cat.attributes, CategoryLocal(Some(UUID.randomUUID()), Some(cat.description), cat.portionSizeMethods))
  }

  def allAsServedSets(): Seq[AsServedHeader] = asServedSets.values.map(s => AsServedHeader(s.id, s.description)).toSeq
  
  def asServedDef(id: String): AsServedSet = asServedSets(id)

  def allGuideImages(): Seq[GuideHeader] = guideImages.values.map(g => GuideHeader(g.id, g.description)).toSeq
  
  def guideDef(id: String): GuideImage =  guideImages(id)
  
  def allDrinkware(): Seq[DrinkwareHeader] = drinkwareSets.values.map(d => DrinkwareHeader(d.id, d.description)).toSeq 

  def drinkwareDef(id: String): DrinkwareSet = drinkwareSets(id)

  def associatedFoodPrompts(foodCode: String, locale: String): Seq[Prompt] = {
    checkLocale(locale)
    prompts(foodCode)
  }

  def brandNames(foodCode: String, locale: String): Seq[String] = {
    checkLocale(locale)
    brandNamesMap(foodCode)
  }

  def allFoodGroups(locale: String) = {
    checkLocale(locale)
    foodGroups.groups
  }

  def foodGroup(id: Int, locale: String) = {
    checkLocale(locale)
    foodGroups.groupMap.get(id)
  }

  def splitList(locale: String) = {
    checkLocale(locale)
    split
  }

  def synsets(locale: String) = {
    checkLocale(locale)
    synSets
  }

  def searchFoods(searchTerm: String, locale: String) = {
    checkLocale(locale)
    foods.foods.filter(f => (f.localData.localDescription.contains(searchTerm) || f.code.contains(searchTerm))).map(mkHeader)
  }

  def searchCategories(searchTerm: String, locale: String) = {
    checkLocale(locale)
    categories.categories.filter(f => (f.description.contains(searchTerm) || f.code.contains(searchTerm))).map(mkHeader)
  }
  
  def nutrientTables() = Seq(NutrientTable("NDNS", "UK National Diet and Nutrition Survey"))  
  
}