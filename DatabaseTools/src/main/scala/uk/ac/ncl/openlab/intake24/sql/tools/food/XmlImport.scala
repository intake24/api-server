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

package uk.ac.ncl.openlab.intake24.sql.tools.food

import anorm._
import upickle.default._
import scala.xml.XML
import java.io.File
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodxml.FoodGroupDef
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.GuideImageDef
import uk.ac.ncl.openlab.intake24.foodxml.BrandDef
import uk.ac.ncl.openlab.intake24.foodxml.DrinkwareDef
import uk.ac.ncl.openlab.intake24.foodxml.PromptDef
import uk.ac.ncl.openlab.intake24.foodxml.CategoryDef
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.FoodGroupLocal
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.SplitList
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import uk.ac.ncl.openlab.intake24.foodxml.XmlFoodRecord
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.foodxml.XmlCategoryRecord
import uk.ac.ncl.openlab.intake24.AsServedSetV1
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.NewLocalCategoryRecord
import uk.ac.ncl.openlab.intake24.NewMainCategoryRecord
import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.IllegalParent
import scala.Left
import scala.Right
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError

class XmlImporter(adminService: FoodDatabaseAdminService) {

  val logger = LoggerFactory.getLogger(getClass)

  val defaultLocale = "en_GB"

  private def checkError[E, T](op: String, result: Either[E, T]) = result match {
    case Left(ParentRecordNotFound(e)) => logger.error(s"$op failed", e)
    case Left(IllegalParent(e)) => logger.error(s"$op failed", e)
    case Left(UnexpectedDatabaseError(e)) => logger.error(s"$op failed", e)
    case Left(e) => logger.error(s"$op failed ${e.toString()}")
    case _ => logger.info(s"$op successful")
  }

  def importFoodGroups(foodGroups: Seq[FoodGroupMain]) = {
    val baseLocaleData = foodGroups.map {
      g => (g.id -> FoodGroupLocal(Some(g.englishDescription)))
    }.toMap

    checkError("Food groups import ", for (
      _ <- adminService.deleteAllFoodGroups().right;
      _ <- adminService.createFoodGroups(foodGroups).right;
      _ <- adminService.createLocalFoodGroups(baseLocaleData, defaultLocale).right
    ) yield ())
  }

  def importFoods(foods: Seq[XmlFoodRecord], categories: Seq[XmlCategoryRecord], associatedFoods: Map[String, Seq[AssociatedFood]], brandNames: Map[String, Seq[String]]) = {

    val parentCategories = {
      val z = foods.foldLeft(Map[String, Set[String]]()) {
        (map, food) => map + (food.code -> Set())
      }

      categories.foldLeft(z) {
        (map, category) =>
          category.foods.foldLeft(map) {
            (map, foodCode) => map + (foodCode -> ((map(foodCode) + category.code)))
          }
      }
    }

    val newFoodRecords = foods.map {
      f => NewMainFoodRecord(f.code, f.description, f.groupCode, f.attributes, parentCategories(f.code).toSeq, Seq())
    }

    val newLocalRecords = foods.map {
      f => (f.code -> NewLocalFoodRecord(Some(f.description), false, f.nutrientTableCodes, f.portionSizeMethods, associatedFoods.getOrElse(f.code, Seq()), brandNames.getOrElse(f.code, Seq())))
    }.toMap

    checkError("Foods import", for (
      _ <- adminService.deleteAllFoods().right;
      _ <- adminService.createFoods(newFoodRecords).right;
      _ <- adminService.createLocalFoodRecords(newLocalRecords, defaultLocale).right
    ) yield ())
  }

  def importCategories(categories: Seq[XmlCategoryRecord]) = {

    val parentCategories = {
      val z = categories.foldLeft(Map[String, Set[String]]()) {
        (map, category) => map + (category.code -> Set())
      }

      categories.foldLeft(z) {
        (map, category) =>
          category.subcategories.foldLeft(map) {
            (map, subcategoryCode) => map + (subcategoryCode -> (map(subcategoryCode) + category.code))
          }
      }
    }

    val newCategoryRecords = categories.map {
      c => NewMainCategoryRecord(c.code, c.description, c.isHidden, c.attributes, parentCategories(c.code).toSeq)
    }

    val newLocalRecords = categories.map {
      c => (c.code -> NewLocalCategoryRecord(Some(c.description), c.portionSizeMethods))
    }.toMap

    checkError("Categories import", for (
      _ <- adminService.deleteAllCategories().right;
      _ <- adminService.createMainCategoryRecords(newCategoryRecords).right;
      _ <- adminService.createLocalCategoryRecords(newLocalRecords, defaultLocale).right
    ) yield ())
  }

  def importAsServedSets(asServed: Seq[AsServedSetV1]) = ???
    /* checkError("As served sets import", for (
      _ <- adminService.deleteAllAsServedSets().right;
      _ <- adminService.createAsServedSets(asServed).right
    ) yield ()) */

  private case class ImageMapArea(id: Int, coords: Seq[Double])
  private case class ImageMapRecord(navigation: Seq[Seq[Int]], areas: Seq[ImageMapArea])

  private def parseImageMaps(imageMapsPath: String) = {
    def parseImageMap(file: File) = {
      logger.debug("Importing image map from " + file.getName)
      val json = scala.io.Source.fromFile(file).mkString
      read[ImageMapRecord](json)
    }

    ((new File(imageMapsPath)).listFiles() match {
      case null => {
        logger.warn("Image maps path does not exist or is not a directory")
        Array[File]()
      }
      case files => files
    }).filter(_.getName.endsWith(".imagemap")).map(parseImageMap)
  }

  def importImageMaps(imageMaps: Seq[ImageMapRecord]) = ???

  def importGuideImages(guideImages: Seq[GuideImage]) = ??? /*{
    checkError("Guide image import", for (
      _ <- adminService.deleteAllGuideImages().right;
      _ <- adminService.createGuideImages(guideImages).right
    ) yield ())
  }*/

  def importDrinkwareSets(drinkwareSets: Seq[DrinkwareSet]) = {
    checkError("Drinkware sets import", for (
      _ <- adminService.deleteAllDrinkwareSets().right;
      _ <- adminService.createDrinkwareSets(drinkwareSets).right
    ) yield ())
  }

  def parseAssociatedFoods(categories: Seq[XmlCategoryRecord], promptsPath: String): Map[String, Seq[AssociatedFood]] = {
    logger.debug("Loading associated food prompts from " + promptsPath)

    val prompts = PromptDef.parseXml(XML.load(promptsPath))

    logger.debug("Indexing foods and categories for associated type resolution" + promptsPath)

    val categoryCodes = categories.map(_.code).toSet

    logger.debug("Resolving associated food/category types")

    prompts.mapValues {
      _.map {
        prompt =>
          if (categoryCodes.contains(prompt.category)) {
            logger.debug(s"Resolved ${prompt.category} as category")
            AssociatedFood(Right(prompt.category), prompt.promptText, prompt.linkAsMain, prompt.genericName)
          } else {
            logger.debug(s"Resolved ${prompt.category} as food")
            AssociatedFood(Left(prompt.category), prompt.promptText, prompt.linkAsMain, prompt.genericName)
          }
      }
    }
  }

  def importSplitList(path: String) {

    logger.debug("Loading split list from " + path)
    val lines = scala.io.Source.fromFile(path).getLines().toSeq

    val splitWords = lines.head.split("\\s+")

    val keepPairs = lines.tail.map {
      line =>
        val words = line.split("\\s+")

        (words.head, words.tail.toSet)

    }.toMap

    checkError("Split list import", for (
      _ <- adminService.deleteSplitList(defaultLocale).right;
      _ <- adminService.createSplitList(SplitList(splitWords, keepPairs), defaultLocale).right
    ) yield ())
  }

  def importSynonymSets(path: String) = {
    logger.debug("Loading synonym sets from " + path)
    val synsets = scala.io.Source.fromFile(path).getLines().toSeq.map(_.split("\\s+").toSet)

    checkError("Synonym sets import", for (
      _ <- adminService.deleteSynsets(defaultLocale).right;
      _ <- adminService.createSynsets(synsets, defaultLocale).right
    ) yield ())
  }

  def importXmlData(dataDirectory: String) = {

    val foodGroups = FoodGroupDef.parseXml(XML.load(dataDirectory + File.separator + "food-groups.xml"))
    val foods = FoodDef.parseXml(XML.load(dataDirectory + File.separator + "foods.xml"))
    val categories = CategoryDef.parseXml(XML.load(dataDirectory + File.separator + "categories.xml"))
    val associatedFoods = parseAssociatedFoods(categories, dataDirectory + File.separator + "prompts.xml")
    val brands = BrandDef.parseXml(XML.load(dataDirectory + File.separator + "brands.xml"))
    val asServedSets = AsServedDef.parseXml(XML.load(dataDirectory + File.separator + "as-served.xml")).values.toSeq
    val guideImages = GuideImageDef.parseXml(XML.load(dataDirectory + File.separator + "guide.xml")).values.toSeq
    val imageMaps = parseImageMaps(dataDirectory + File.separator + "CompiledImageMaps")
    val drinkwareSets = DrinkwareDef.parseXml(XML.load(dataDirectory + File.separator + "drinkware.xml")).values.toSeq

    adminService.deleteLocale(defaultLocale)
    adminService.createLocale(Locale(defaultLocale, "United Kingdom", "United Kingdom", "en_GB", "en", "gb", None))

    importCategories(categories)

    importFoodGroups(foodGroups.map(g => FoodGroupMain(g.id, g.description)))
    importAsServedSets(asServedSets)
    importGuideImages(guideImages)
    /// importImageMaps(imageMaps)
    importDrinkwareSets(drinkwareSets)

    importFoods(foods, categories, associatedFoods, brands)

    importSplitList(dataDirectory + File.separator + "split_list")
    importSynonymSets(dataDirectory + File.separator + "synsets")
  }
}

object XmlImport extends App with WarningMessage with DatabaseConnection {

  trait Options extends ScallopConf {
    version("Intake24 XML to SQL food database migration tool 16.8")

    val xmlPath = opt[String](required = true, noshort = true)
  }

  val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  displayWarningMessage("THIS WILL DESTROY ALL FOOD AND CATEGORY RECORDS IN THE DATABASE!")

  val dataSource = getDataSource(options)

  val adminService = new FoodDatabaseAdminImpl(dataSource)

  val importer = new XmlImporter(adminService)

  importer.importXmlData(options.xmlPath())
}
