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

package uk.ac.ncl.openlab.intake24.foodsql.tools

import anorm._
import upickle.default._
import java.sql.DriverManager
import scala.xml.XML
import java.io.File
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import java.sql.Connection
import upickle.Invalid
import uk.ac.ncl.openlab.intake24.foodxml.FoodGroupDef
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.GuideImageDef
import uk.ac.ncl.openlab.intake24.foodxml.BrandDef
import uk.ac.ncl.openlab.intake24.foodxml.DrinkwareDef
import uk.ac.ncl.openlab.intake24.foodxml.PromptDef
import uk.ac.ncl.openlab.intake24.foodxml.CategoryDef
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import java.sql.BatchUpdateException
import org.rogach.scallop.ScallopConf
import java.util.Properties
import java.io.BufferedReader
import java.io.InputStreamReader

import java.util.UUID
import uk.ac.ncl.openlab.intake24.foodsql.Util
import org.postgresql.util.PSQLException
import org.rogach.scallop.ScallopOption
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl

import uk.ac.ncl.openlab.intake24.FoodGroupLocal

import uk.ac.ncl.openlab.intake24.LocalFoodRecord

import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.SplitList
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import uk.ac.ncl.openlab.intake24.foodxml.XmlFoodRecord
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.foodxml.XmlCategoryRecord

class XmlImporter(adminService: FoodDatabaseAdminService) {

  val logger = LoggerFactory.getLogger(getClass)

  val defaultLocale = "en_GB"

  def importFoodGroups(foodGroups: Seq[FoodGroupMain]) = {
    logger.info("Deleting existing food groups")

    adminService.deleteAllFoodGroups()

    if (!foodGroups.isEmpty) {
      logger.info("Writing " + foodGroups.size + " food groups to database")

      adminService.createFoodGroups(foodGroups)

      val baseLocaleData = foodGroups.map {
        g => (g.id -> FoodGroupLocal(Some(g.englishDescription)))
      }.toMap

      adminService.createLocalFoodGroups(baseLocaleData, defaultLocale)

    } else
      logger.warn("Food groups file contains no records")
  }
  
  def buildFoodParentCategories(categories: Seq[XmlCategoryRecord]) = {
    
    val z = Map[String, Set[String]]()
    
    categories.foldLeft(z) {
      (map, record) => record.foods.foldLeft(z) {
        (map, foodCode) => map + (foodCode -> (map.getOrElse(foodCode, Set()) + record.code))
      }      
    }
    
  }

  def importFoods(foods: Seq[XmlFoodRecord], categories: Seq[XmlCategoryRecord], associatedFoods: Map[String, Seq[AssociatedFood]], brandNames: Map[String, Seq[String]]) = {
    
    

    val newFoodRecords = foods.map {
      f => NewFood(f.code, f.description, f.groupCode, f.attributes)
    }

    val newLocalRecords = foods.map {
      f => (f.code -> NewLocalFoodRecord(Some(f.description), false, f.nutrientTableCodes, f.portionSizeMethods, associatedFoods.getOrElse(f.code, Seq()), brandNames.getOrElse(f.code, Seq())))
    }.toMap

    (for (
      _ <- adminService.deleteAllFoods().right;
      _ <- adminService.createFoods(newFoodRecords).right;
      _ <- adminService.createLocalFoods(newLocalRecords, defaultLocale).right
    ) yield ()) match {
      case Left(DatabaseError(message, _)) => throw new RuntimeException(s"Failed to import foods due to database error: $message")
      case _ => logger.info("Foods import successful")
    }
  }

  def importCategories(categoriesPath: String) = {
    logger.info("Loading categories from " + categoriesPath)

    val categories = CategoryDef.parseXml(XML.load(categoriesPath))

    val newCategoryRecords = categories.map {
      c => NewCategory(c.code, c.description, c.isHidden, c.attributes)
    }

    val newLocalRecords = categories.map {
      c => (c.code -> LocalCategoryRecord(None, Some(c.description), c.portionSizeMethods))
    }.toMap

    (for (
      _ <- adminService.deleteAllCategories().right;
      _ <- adminService.createCategories(newCategoryRecords).right;
      _ <- adminService.createLocalCategories(newLocalRecords, defaultLocale).right
    ) yield ()) match {
      case Left(DatabaseError(message, _)) => throw new RuntimeException(s"Failed to import categories due to database error: $message")
      case _ => logger.info("Categories import successful")
    }
  }

  def importAsServed(asServedPath: String) = {
    logger.info("Loading as served image definitions from " + asServedPath)

    val asServed = AsServedDef.parseXml(XML.load(asServedPath)).values.toSeq.sortBy(_.id)

    (for (
      _ <- adminService.deleteAllAsServedSets().right;
      _ <- adminService.createAsServedSets(asServed).right
    ) yield ()) match {
      case Left(DatabaseError(message, _)) => throw new RuntimeException(s"Failed to import as served image sets due to database error: $message")
      case _ => logger.info("As served image sets import successful")
    }
  }

  private case class ImageMapArea(id: Int, coords: Seq[Double])
  private case class ImageMapRecord(navigation: Seq[Seq[Int]], areas: Seq[ImageMapArea])

  private def importImageMap(file: File) = {
    logger.debug("Importing image map from " + file.getName)
    val json = scala.io.Source.fromFile(file).mkString
    logger.debug(read[ImageMapRecord](json).toString())
  }

  def importGuide(guidePath: String, imageMapsPath: String) = {
    logger.info("Loading guide image definitions from " + guidePath)
    val guideImages = GuideImageDef.parseXml(XML.load(guidePath)).values.toSeq

    (for (
      _ <- adminService.deleteAllGuideImages().right;
      _ <- adminService.createGuideImages(guideImages).right
    ) yield ()) match {
      case Left(DatabaseError(message, _)) => throw new RuntimeException(s"Failed to import guide images due to database error: $message")
      case _ => logger.info("As served image sets import successful")
    }

    logger.info("Importing guide image maps from " + imageMapsPath)

    ((new File(imageMapsPath)).listFiles() match {
      case null => {
        logger.warn("Image maps path does not exist or is not a directory")
        Array[File]()
      }
      case files => files
    }).filter(_.getName.endsWith(".imagemap")).foreach(importImageMap)
  }

  def importDrinkware(drinkwarePath: String) = {

    logger.info("Loading drinkware definitions from " + drinkwarePath)
    val drinkware = DrinkwareDef.parseXml(XML.load(drinkwarePath)).values.toSeq
    (for (
      _ <- adminService.deleteAllDrinkwareSets().right;
      _ <- adminService.createDrinkwareSets(drinkware).right
    ) yield ()) match {
      case Left(DatabaseError(message, _)) => throw new RuntimeException(s"Failed to import guide images due to database error: $message")
      case _ => logger.info("As served image sets import successful")
    }
  }

  def parseAssociatedFoods(categories: Seq[XmlCategoryRecord], promptsPath: String): Map[String, Seq[AssociatedFood]] = {

    logger.info("Loading associated food prompts from " + promptsPath)

    val prompts = PromptDef.parseXml(XML.load(promptsPath))

    logger.info("Indexing foods and categories for associated type resolution" + promptsPath)

    val categoryCodes = categories.map(_.code).toSet

    logger.info("Resolving associated food/category types")

    prompts.mapValues {
      _.map {
        prompt =>
          if (categoryCodes.contains(prompt.category)) {
            logger.info(s"Resolved ${prompt.category} as category")
            AssociatedFood(Right(prompt.category), prompt.promptText, prompt.linkAsMain, prompt.genericName)
          } else {
            logger.info(s"Resolved ${prompt.category} as food")
            AssociatedFood(Left(prompt.category), prompt.promptText, prompt.linkAsMain, prompt.genericName)
          }
      }
    }
  }

  def importSplitList(path: String) {

    logger.info("Loading split list from " + path)
    val lines = scala.io.Source.fromFile(path).getLines().toSeq

    val splitWords = lines.head.split("\\s+")

    val keepPairs = lines.tail.map {
      line =>
        val words = line.split("\\s+")

        (words.head, words.tail.toSet)

    }.toMap

    (for (
      _ <- adminService.deleteSplitList(defaultLocale).right;
      _ <- adminService.createSplitList(SplitList(splitWords, keepPairs), defaultLocale).right
    ) yield ()) match {
      case Left(DatabaseError(message, _)) => throw new RuntimeException(s"Failed to import split list due to database error: $message")
      case _ => logger.info("Split list import successful")
    }
  }

  def importSynonymSets(path: String) = {
    logger.info("Loading synonym sets from " + path)
    val synsets = scala.io.Source.fromFile(path).getLines().toSeq.map(_.split("\\s+").toSet)

    (for (
      _ <- adminService.deleteSynsets(defaultLocale).right;
      _ <- adminService.createSynsets(synsets, defaultLocale).right
    ) yield ()) match {
      case Left(DatabaseError(message, _)) => throw new RuntimeException(s"Failed to import synonym sets due to database error: $message")
      case _ => logger.info("Synonym sets import successful")
    }
  }

  def importXmlData(dataDirectory: String) = {
    
    val foodGroups = FoodGroupDef.parseXml(XML.load(dataDirectory + File.separator + "food-groups.xml"))
    val foods = FoodDef.parseXml(XML.load(dataDirectory + File.separator + "foods.xml"))
    val categories = CategoryDef.parseXml(XML.load(dataDirectory + File.separator + "categories.xml"))
    val associatedFoods = parseAssociatedFoods(categories, dataDirectory + File.separator + "prompts.xml")
    val brands = BrandDef.parseXml(XML.load(dataDirectory + File.separator + "brands.xml"))
    
    importFoodGroups(foodGroups)
    importFoods(foods, categories, associatedFoods, brands)
    
    
    importCategories()
    importAsServed(dataDirectory + File.separator + "as-served.xml")
    importGuide(dataDirectory + File.separator + "guide.xml", dataDirectory + File.separator + "CompiledImageMaps")

    importDrinkware(dataDirectory + File.separator + "drinkware.xml")

    importSplitList(dataDirectory + File.separator + "split_list")
    importSynonymSets(dataDirectory + File.separator + "synsets")
  }
}

trait Options extends ScallopConf {
  version("Intake24 XML to SQL food database migration tool 16.8")

  val xmlPath = opt[String](required = true, noshort = true)
}

object XmlImport extends App with WarningMessage with DatabaseConnection {

  val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage("THIS WILL DESTROY ALL FOOD AND CATEGORY RECORDS IN THE DATABASE!")

  val dataSource = getDataSource(options)

  val adminService = new FoodDatabaseAdminImpl(dataSource)

  implicit val dbConn = dataSource.getConnection

  val importer = new XmlImporter(adminService)

  importer.importXmlData(options.xmlPath())
}
