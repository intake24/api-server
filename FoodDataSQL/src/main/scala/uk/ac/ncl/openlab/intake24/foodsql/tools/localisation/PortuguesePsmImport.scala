package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConverters._
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter

import org.slf4j.LoggerFactory
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseConnection

object PortuguesePsmImport extends App with WarningMessage with DatabaseConnection {

  val logger = LoggerFactory.getLogger(PortuguesePsmImport.getClass)

  val referenceLocaleId = "en_GB"

  val portugueseLocaleId = "pt_PT"

  trait Options extends ScallopConf {
    version("Intake24 Portuguese PSM data import tool 16.8")

    val csvPath = opt[String](required = true, noshort = true)
    val nutrientCsvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  // displayWarningMessage("WARNING: This will override existing portion size methods for Portuguese locale")

  val dataSource = getDataSource(options)

  val dataService = new FoodDatabaseAdminImpl(dataSource)

  val indexDataService = new FoodIndexDataImpl(dataSource)

  logger.info("Retrieving as served set headers")
  val asServedSetKeys = dataService.listAsServedSets().right.get.keySet

  logger.info("Building as served image index")

  val asServedSets = asServedSetKeys.map {
    as => dataService.getAsServedSet(as).right.get
  }

  logger.info(s"Retrieving indexable food records for $referenceLocaleId")

  val indexableFoods = indexDataService.indexableFoods(referenceLocaleId).right.get

  logger.info("Building PSM index")

  case class PsmIndex(guide: Map[String, PortionSizeMethod], asServed: Map[String, PortionSizeMethod])

  val psmIndex = indexableFoods.foldLeft(PsmIndex(Map(), Map())) {
    (index, header) =>

      dataService.getFoodRecord(header.code, referenceLocaleId) match {
        case Right(record) =>
          record.local.portionSize.foldLeft(index) {
            (index, psm) =>
              psm.method match {
                case "guide-image" => {
                  val id = psm.parameters.find(_.name == "guide-image-id").get.value
                  if (index.guide.contains(id))
                    index
                  else
                    index.copy(guide = index.guide + (id -> psm))
                }
                case "as-served" => {
                  val id1 = psm.parameters.find(_.name == "serving-image-set").get.value
                  val id2 = psm.parameters.find(_.name == "leftovers-image-set").map(_.value)

                  val i1 = if (index.asServed.contains(id1)) index else index.copy(asServed = index.asServed + (id1 -> psm))

                  id2 match {
                    case Some(id) => {
                      if (index.asServed.contains(id)) i1 else i1.copy(asServed = i1.asServed + (id -> psm))
                    }
                    case None => i1
                  }
                }
                case _ => index
              }
          }

        case _ => throw new RuntimeException(s"Couldn't retrieve record for ${header.toString}")
      }
  }

  logger.info(s"Retrieving indexable food records for $portugueseLocaleId")

  val ptIndexableFoods = indexDataService.indexableFoods(portugueseLocaleId).right.get

  logger.info("Building PT_INSA to Intake24 codes map")

  val reader2 = new CSVReader(new FileReader(options.nutrientCsvPath()))

  val ptFoodCodeToIdMap = reader2.readAll().asScala.foldLeft(Map[String, String]()) {
    (map, row) => if (row(1).nonEmpty) map + (row(1) -> row(0)) else map
  }

  logger.debug(ptFoodCodeToIdMap.toString())

  reader2.close()

  val ptToIntakeCodesMap = ptIndexableFoods.foldLeft(Map[String, String]()) {
    (map, foodHeader) =>
      val record = dataService.getFoodRecord(foodHeader.code, portugueseLocaleId).right.get

      record.local.nutrientTableCodes.get("PT_INSA") match {
        case Some(code) => {
          map + (code -> record.main.code)
        }
        case None => map
      }
  }

  logger.debug(ptToIntakeCodesMap.toString())

  val reader = new CSVReader(new FileReader(options.csvPath()))

  // Intake food code -> Guide or AS reference
  val z = Map[String, Seq[String]]()

  val references = reader.readAll().asScala.foldLeft(z) {
    (map, row) =>

      val guideOrAsServedRef = row.head
      val foodRefs = row.tail.filterNot(_.isEmpty()).map(code => ptFoodCodeToIdMap.get(code).flatMap {
        code =>
          if (ptToIntakeCodesMap.contains(code))
            Some(ptToIntakeCodesMap(code))
          else {
            logger.warn(s"Pt food ID $code present in PSM table could not be mapped to Intake24 code")
            None
          }
      }.getOrElse(code))

      foodRefs.foldLeft(map) {
        (map, foodRef) => map + (foodRef -> (guideOrAsServedRef +: map.getOrElse(foodRef, Seq[String]())))
      }
  }

  reader.close()

  def guessAsServed(name: String) = {
    logger.info(s"Trying to guess as served set from $name")
    asServedSets.find(set => !set.description.contains("leftover") && set.images.exists(_.url.contains(name))).map(_.id) match {
      case Some(set) => {
        logger.info(s"Guessed as $set")
        Some(set)
      }
      case None => {
        logger.info(s"No appropriate set found")
        None
      }
    }
  }

  val methods = references.foldLeft((Map[String, Seq[PortionSizeMethod]](), Seq[String]())) {
    case ((map, badRefs), (foodCode, psmRefs)) =>
      
      var r = Seq[String]()
      
      val psm = psmRefs.map {
        ref =>

          val result = psmIndex.guide.get(ref).orElse {
            guessAsServed(ref).flatMap(set => psmIndex.asServed.get(set))
          }

          if (result.isEmpty) {
            logger.warn(s"$ref is not a known guide or as served image id, ignoring")
            r +:= ref
          }

          result
      }.flatten
      
      (map + (foodCode -> psm), badRefs ++ r)
  }
  
  println ("BAD REFS")
  methods._2.toSet.foreach(println)

  methods._1.keySet.foreach {
    key =>

      // logger.info(s"Updating $key")

      dataService.getFoodRecord(key, portugueseLocaleId) match {
        case Right(record) => {
          val updatedLocal = record.local.toUpdate.copy(portionSize = methods._1(key))
          dataService.updateLocalFoodRecord(key, updatedLocal, portugueseLocaleId)
        }
        case _ => logger.warn(s"Couldn't retrieve food record for Intake24 code $key")
      }
  }
}