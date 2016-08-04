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
import uk.ac.ncl.openlab.intake24.foodsql.admin.AdminFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.services.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.FoodGroupLocal

import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.services.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.NewFood

class XmlImporter(adminService: FoodDatabaseAdminService, implicit val dbConn: Connection) {

  val logger = LoggerFactory.getLogger(getClass)

  val defaultLocale = "en_GB"

  def importFoodGroups(foodGroupsPath: String) = {
    logger.info("Deleting existing food groups")

    adminService.deleteAllFoodGroups()

    logger.info("Loading food groups from " + foodGroupsPath)

    val foodGroups = FoodGroupDef.parseXml(XML.load(foodGroupsPath))

    if (!foodGroups.isEmpty) {
      logger.info("Writing " + foodGroups.size + " food groups to database")

      adminService.createFoodGroups(foodGroups)

      val baseLocaleData = foodGroups.map {
        g => (g.id -> g.englishDescription)
      }.toMap

      adminService.createLocalFoodGroups(baseLocaleData, defaultLocale)

    } else
      logger.warn("Food groups file contains no records")
  }

  def importFoods(foodsPath: String) = {
    logger.info("Loading foods from " + foodsPath)

    val foods = FoodDef.parseXml(XML.load(foodsPath))

    logger.info("Deleting existing food records")

    adminService.deleteAllFoods()

    val newFoodRecords = foods.map {
      f => NewFood(f.main.code, f.main.englishDescription, f.main.groupCode, f.main.attributes)
    }

    val newLocalRecords = foods.map {
      f => (f.main.code -> LocalFoodRecord(None, f.local.localDescription, f.local.doNotUse, f.local.nutrientTableCodes, f.local.portionSize))
    }.toMap
    
    adminService.createFoods(newFoodRecords).right.flatMap(_ => adminService.createLocalFoods(newLocalRecords)) match {
      case Left(DatabaseError(message)) => throw new RuntimeException(s"Failed to import foods: $message")
      case _ => logger.info("Foods import successful")
    }
  }

  def importCategories(categoriesPath: String) = {
    logger.info("Deleting existing categories")

    SQL("DELETE FROM categories").execute()

    logger.info("Loading categories from " + categoriesPath)

    val categories = CategoryDef.parseXml(XML.load(categoriesPath))

    if (!categories.isEmpty) {

      logger.info("Writing " + categories.size + " categories to database")

      val categoryParams =
        categories.map(c => Seq[NamedParameter]('code -> c.code, 'description -> c.description, 'is_hidden -> c.isHidden, 'version -> c.version))

      BatchSql(Queries.categoriesInsert, categoryParams).execute()

      val localCategoryParams =
        categories.map(c => Seq[NamedParameter]('category_code -> c.code, 'locale_id -> defaultLocale, 'local_description -> c.description, 'version -> UUID.randomUUID()))

      BatchSql(Queries.categoriesLocalInsert, localCategoryParams).execute()

      val foodCategoryParams =
        categories.flatMap(c => c.foods.map(f => Seq[NamedParameter]('food_code -> f, 'category_code -> c.code)))

      if (!foodCategoryParams.isEmpty) {
        logger.info("Writing " + foodCategoryParams.size + " food parent category records")
        BatchSql(Queries.foodsCategoriesInsert, foodCategoryParams).execute()
      } else
        logger.warn("No foods contained in any of the categories")

      val categoryCategoryParams =
        categories.flatMap(c => c.subcategories.map(sc => Seq[NamedParameter]('subcategory_code -> sc, 'category_code -> c.code)))

      if (!categoryCategoryParams.isEmpty) {
        logger.info("Writing " + foodCategoryParams.size + " category parent category records")
        BatchSql(Queries.categoriesCategoriesInsert, categoryCategoryParams).execute()
      } else
        logger.warn("No subcategories contained in any of the categories")

      val categoryAttributeParams =
        categories.map(c => Seq[NamedParameter]('category_code -> c.code, 'same_as_before_option -> c.attributes.sameAsBeforeOption,
          'ready_meal_option -> c.attributes.readyMealOption, 'reasonable_amount -> c.attributes.reasonableAmount))

      BatchSql(Queries.categoriesAttributesInsert, categoryAttributeParams).execute()

      val psmParams =
        categories.flatMap(c => c.portionSizeMethods.map(ps => Seq[NamedParameter]('category_code -> c.code, 'locale_id -> defaultLocale,
          'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes)))

      if (!psmParams.isEmpty) {
        logger.info("Writing " + psmParams.size + " category portion size method definitions")

        val statement = BatchSql(Queries.categoriesPortionSizeMethodsInsert, psmParams).getFilledStatement(dbConn, true)

        statement.executeBatch()

        val rs = statement.getGeneratedKeys()
        val buf = ArrayBuffer[Long]()

        while (rs.next()) {
          buf += rs.getLong(1)
        }

        val psmParamParams = categories.flatMap(_.portionSizeMethods).zip(buf).flatMap {
          case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
        }

        if (!psmParamParams.isEmpty) {
          logger.info("Writing " + psmParamParams.size + " category portion size method parameters")
          BatchSql(Queries.categoriesPortionSizeMethodParamsInsert, psmParamParams).execute()
        } else
          logger.warn("No category portion size methor parameters found")
      } else
        logger.warn("No category portion size method records found")
    } else
      logger.warn("Categories file contains no records")
  }

  def importAsServed(asServedPath: String) = {
    logger.info("Deleting existing as served image definitions")

    SQL("DELETE FROM as_served_sets").execute()

    logger.info("Loading as served image definitions from " + asServedPath)
    val asServed = AsServedDef.parseXml(XML.load(asServedPath)).values.toSeq.sortBy(_.id)

    if (!asServed.isEmpty) {
      logger.info("Writing " + asServed.size + " as served sets to database")
      val asServedSetParams = asServed.map(set => Seq[NamedParameter]('id -> set.id, 'description -> set.description))
      BatchSql("""INSERT INTO as_served_sets VALUES({id}, {description})""", asServedSetParams).execute()

      val asServedImageParams = asServed.flatMap(set => set.images.map(image => Seq[NamedParameter]('as_served_set_id -> set.id, 'weight -> image.weight, 'url -> image.url)))
      if (!asServedImageParams.isEmpty) {
        logger.info("Writing " + asServedImageParams.size + " as served images to database")
        BatchSql("""INSERT INTO as_served_images VALUES(DEFAULT, {as_served_set_id}, {weight}, {url})""", asServedImageParams).execute()
      } else
        logger.warn("As served sets contain no image references")
    } else
      logger.warn("As served file contains no records")
  }

  private case class ImageMapArea(id: Int, coords: Seq[Double])
  private case class ImageMapRecord(navigation: Seq[Seq[Int]], areas: Seq[ImageMapArea])

  private def importImageMap(file: File) = {
    logger.debug("Importing image map from " + file.getName)
    val json = scala.io.Source.fromFile(file).mkString
    logger.debug(read[ImageMapRecord](json).toString())
  }

  def importGuide(guidePath: String, imageMapsPath: String) = {
    logger.info("Deleting existing guide image definitions")

    SQL("DELETE FROM guide_images").execute()

    logger.info("Loading guide image definitions from " + guidePath)
    val guideImages = GuideImageDef.parseXml(XML.load(guidePath))

    if (!guideImages.isEmpty) {
      logger.info("Writing " + guideImages.size + " guide images to database")

      val guideImageParams = guideImages.map {
        case (guide_id, image) => Seq[NamedParameter]('id -> guide_id, 'description -> image.description, 'base_image_url -> (guide_id + ".jpg"))
      }.toSeq

      BatchSql("""INSERT INTO guide_images VALUES ({id},{description},{base_image_url})""", guideImageParams).execute()

      val weightParams = guideImages.flatMap {
        case (guide_id, image) =>
          image.weights.map {
            weight =>
              Seq[NamedParameter]('guide_image_id -> guide_id, 'object_id -> weight.objectId, 'description -> weight.description, 'weight -> weight.weight)
          }
      }.toSeq

      if (!weightParams.isEmpty) {
        logger.info("Writing " + weightParams.size + " guide image weight records to database")
        BatchSql("""INSERT INTO guide_image_weights VALUES (DEFAULT,{guide_image_id},{object_id},{description},{weight})""", weightParams).execute()
      } else
        logger.warn("Guide image file contains no object weight records")
    } else
      logger.warn("Guide file contains no records")

    logger.info("Importing guide image maps from " + imageMapsPath)

    ((new File(imageMapsPath)).listFiles() match {
      case null => {
        logger.warn("Image maps path does not exist or is not a directory")
        Array[File]()
      }
      case files => files
    }).filter(_.getName.endsWith(".imagemap")).foreach(importImageMap)
  }

  def importBrands(brandsPath: String) = {
    logger.info("Deleting existing brand definitions")

    SQL("DELETE FROM brands").execute()

    logger.info("Loading brands definitions from " + brandsPath)
    val brands = BrandDef.parseXml(XML.load(brandsPath))

    if (!brands.isEmpty) {
      logger.info("Writing " + brands.size + " brands to database")
      val brandParams = brands.keySet.toSeq.flatMap(k => brands(k).map(name => Seq[NamedParameter]('food_code -> k, 'locale_id -> defaultLocale, 'name -> name)))
      try {
        BatchSql("""INSERT INTO brands VALUES(DEFAULT, {food_code}, {locale_id}, {name})""", brandParams).execute()
      } catch {
        case e: BatchUpdateException => throw new RuntimeException(e.getMessage, e.getNextException)
      }
    } else
      logger.warn("Brands file contains no records")
  }

  def importDrinkware(drinkwarePath: String) = {
    logger.info("Deleting existing drinkware definitions")

    SQL("DELETE FROM drinkware_sets").execute()

    logger.info("Loading drinkware definitions from " + drinkwarePath)
    val drinkware = DrinkwareDef.parseXml(XML.load(drinkwarePath))

    if (!drinkware.isEmpty) {

      logger.info("Writing " + drinkware.size + " drinkware sets to database")
      val drinkwareParams = drinkware.keySet.toSeq.map {
        id =>
          val dw = drinkware(id)
          Seq[NamedParameter]('id -> id, 'description -> dw.description, 'guide_image_id -> dw.guide_id)
      }

      BatchSql("""INSERT INTO drinkware_sets VALUES ({id}, {description}, {guide_image_id})""", drinkwareParams).execute()

      val drinkwareScaleParams = drinkware.keySet.foreach {
        id =>
          val dw = drinkware(id)
          dw.scaleDefs.foreach {
            scale =>
              val scaleId = SQL("""INSERT INTO drinkware_scales VALUES (DEFAULT, {drinkware_set_id}, {width}, {height}, {empty_level}, {full_level}, {choice_id}, {base_image_url}, {overlay_image_url})""")
                .on('drinkware_set_id -> id, 'width -> scale.width, 'height -> scale.height, 'empty_level -> scale.emptyLevel, 'full_level -> scale.fullLevel,
                  'choice_id -> scale.choice_id, 'base_image_url -> scale.baseImage, 'overlay_image_url -> scale.overlayImage)
                .executeInsert()
                .get

              val volumeSampleParams = scale.vf.sortedSamples.map {
                case (fill, volume) =>
                  Seq[NamedParameter]('scale_id -> scaleId, 'fill -> fill, 'volume -> volume)
              }

              if (!volumeSampleParams.isEmpty) {
                logger.info("Writing " + volumeSampleParams.size + " volume sample records to database")
                BatchSql("""INSERT INTO drinkware_volume_samples VALUES (DEFAULT, {scale_id}, {fill}, {volume})""", volumeSampleParams).execute()
              } else
                logger.warn("Drinkware file contains no volume samples")
          }
      }
    } else
      logger.warn("Drinkware file contains no records")
  }

  def importAssociatedFoodPrompts(foodsPath: String, categoryPath: String, promptsPath: String) = {
    logger.info("Deleting existing associated food prompts")

    SQL("DELETE FROM associated_foods").execute()

    logger.info("Loading associated food prompts from " + promptsPath)

    val prompts = PromptDef.parseXml(XML.load(promptsPath))

    logger.info("Indexing foods and categories for associated type resolution" + promptsPath)

    val foods = FoodDef.parseXml(XML.load(foodsPath)).map(_.main.code).toSet
    val categories = CategoryDef.parseXml(XML.load(categoryPath)).map(_.code).toSet

    if (!prompts.isEmpty) {
      logger.info("Writing " + prompts.size + " associated food prompts to database")

      val promptParams = prompts.keySet.toSeq.flatMap {
        foodCode =>

          prompts(foodCode).map {
            prompt =>

              val (assocFoodCode, assocCategoryCode) =
                if (categories.contains(prompt.category)) {
                  logger.info(s"Resolved ${prompt.category} as category")
                  (None, Some(prompt.category))
                } else {
                  logger.info(s"Resolved ${prompt.category} as food")
                  (Some(prompt.category), None)
                }

              Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> defaultLocale, 'associated_food_code -> assocFoodCode, 'associated_category_code -> assocCategoryCode, 'text -> prompt.promptText, 'link_as_main -> prompt.linkAsMain,
                'generic_name -> prompt.genericName)
          }
      }

      try {
        BatchSql("""INSERT INTO associated_foods VALUES (DEFAULT, {food_code}, {locale_id}, {associated_food_code}, {associated_category_code}, {text}, {link_as_main}, {generic_name})""", promptParams).execute()
      } catch {
        case e: java.sql.BatchUpdateException => e.getNextException.printStackTrace()
      }
    } else
      logger.warn("Associated food prompts file contains no records")
  }

  def importSplitList(path: String) {
    logger.info("Deleting existing split list")

    SQL("DELETE FROM split_words").execute()
    SQL("DELETE FROM split_list").execute()

    logger.info("Loading split list from " + path)
    val lines = scala.io.Source.fromFile(path).getLines().toSeq

    if (!lines.isEmpty) {

      logger.info("Writing split list to database")

      val splitWords = lines.head.split("\\s+")

      SQL("""INSERT INTO split_words VALUES (DEFAULT, {locale}, {words})""")
        .on('locale -> "en_GB", 'words -> splitWords.mkString(" "))
        .executeInsert()

      val splitListParams = lines.tail.map {
        line =>
          val words = line.split("\\s+")
          Seq[NamedParameter]('locale -> "en_GB", 'first_word -> words.head, 'words -> words.tail.mkString(" "))
      }

      if (!splitListParams.isEmpty)
        BatchSql("""INSERT INTO split_list VALUES (DEFAULT, {locale}, {first_word}, {words})""", splitListParams).execute()
      else
        logger.warn("Split list parameter list is empty")
    } else
      logger.warn("Split list file is empty")
  }

  def importSynonymSets(path: String) = {
    logger.info("Deleting existing synonym sets")

    SQL("DELETE FROM synonym_sets").execute()

    logger.info("Loading synonym sets from " + path)
    val lines = scala.io.Source.fromFile(path).getLines().toSeq

    if (!lines.isEmpty) {
      logger.info("Writing synonym sets to database")

      val synonymSetsParams = lines.map {
        line =>
          val words = line.split("\\s+")
          Seq[NamedParameter]('locale -> "en_GB", 'synonyms -> words.mkString(" "))
      }

      BatchSql("""INSERT INTO synonym_sets VALUES (DEFAULT, {locale}, {synonyms})""", synonymSetsParams).execute()
    } else
      logger.warn("Synonym sets file is empty")
  }

  def importXmlData(dataDirectory: String) = {
    importFoodGroups(dataDirectory + File.separator + "food-groups.xml")
    importFoods(dataDirectory + File.separator + "foods.xml")
    importCategories(dataDirectory + File.separator + "categories.xml")
    importAsServed(dataDirectory + File.separator + "as-served.xml")
    importGuide(dataDirectory + File.separator + "guide.xml", dataDirectory + File.separator + "CompiledImageMaps")
    importBrands(dataDirectory + File.separator + "brands.xml")
    importDrinkware(dataDirectory + File.separator + "drinkware.xml")
    importAssociatedFoodPrompts(dataDirectory + File.separator + "foods.xml", dataDirectory + File.separator + "categories.xml", dataDirectory + File.separator + "prompts.xml")
    importSplitList(dataDirectory + File.separator + "split_list")
    importSynonymSets(dataDirectory + File.separator + "synsets")
  }
}

trait Options extends ScallopConf {
  version("Intake24 XML to SQL food database migration tool 16.7-SNAPSHOT")

  val xmlPath = opt[String](required = true, noshort = true)
}

object XmlImport extends App with WarningMessage with DatabaseConnection {

  val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage("THIS WILL DESTROY ALL FOOD AND CATEGORY RECORDS IN THE DATABASE!")

  val dataSource = getDataSource(options)

  val adminService = new AdminFoodDataServiceSqlImpl(dataSource)

  implicit val dbConn = dataSource.getConnection

  val importer = new XmlImporter()

  importer.importXmlData(options.xmlPath())
}
