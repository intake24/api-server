package uk.ac.ncl.openlab.intake24.foodxml

import com.google.inject.name.Named
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.services.LookupServiceUtil
import com.google.inject.Singleton
import java.io.File

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
class XmlDataSource @Inject() (@Named("xml-data-path") dataPath: String) {
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

  val log = LoggerFactory.getLogger(classOf[XmlDataSource])

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
}