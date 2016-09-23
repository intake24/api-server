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
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.foodsql.tools.ErrorHandler
import uk.ac.ncl.openlab.intake24.UserFoodHeader
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndexDataService

class PortuguesePsmTableParser extends PortionSizeTableParser with ErrorHandler {

  private val baseLocaleCode = "en_GB"
  private val localeCode = "pt_PT"
  
  private val logger = LoggerFactory.getLogger(classOf[PortuguesePsmTableParser])

  def parsePortionSizeMethodsTable(csvPath: String, nutrientTableCsvPath: String, localToIntakeCodes: Map[String, String],  indexableFoods: Seq[UserFoodHeader], 
      dataService: FoodDatabaseAdminService): Map[String, Seq[PortionSizeMethod]] = {
    
    logger.info("Portuguese portion size methods intentionally ignored")
    
    Map()

    /* logger.info("Building PT food code to ID map")

    val nutrientTableReader = new CSVReader(new FileReader(nutrientTableCsvPath))

    val ptFoodCodeToIdMap = nutrientTableReader.readAll().asScala.foldLeft(Map[String, String]()) {
      (map, row) => if (row(1).nonEmpty) map + (row(1) -> row(0)) else map
    }

    nutrientTableReader.close()

    logger.info("Loading Portuguese portion size method table")

    val psmTableReader = new CSVReader(new FileReader(csvPath))

    val psmTable = psmTableReader.readAll().asScala.foldLeft(Map[String, Seq[String]]()) {
      (map, row) =>

        val guideOrAsServedRef = row.head
        val foodRefs = row.tail.filterNot(_.isEmpty()).map(code => ptFoodCodeToIdMap.get(code).flatMap {
          code =>
            if (localToIntakeCodes.contains(code))
              Some(localToIntakeCodes(code))
            else {
              logger.warn(s"Pt food ID $code present in PSM table could not be mapped to Intake24 code")
              None
            }
        }.getOrElse(code))

        foodRefs.foldLeft(map) {
          (map, foodRef) => map + (foodRef -> (guideOrAsServedRef +: map.getOrElse(foodRef, Seq[String]())))
        }
    }

    psmTableReader.close()

    logger.info("Loading as served set headers")
    val asServedSetKeys = throwOnError(dataService.listAsServedSets()).keySet

    logger.info("Building as served image index")

    val asServedSets = asServedSetKeys.map {
      as => throwOnError(dataService.getAsServedSet(as))
    }

    logger.info("Building PSM index (this will take a while...)")

    case class PsmIndex(guide: Map[String, PortionSizeMethod], asServed: Map[String, PortionSizeMethod])

    val psmIndex = indexableFoods.foldLeft(PsmIndex(Map(), Map())) {
      (index, header) =>

        dataService.getFoodRecord(header.code, baseLocaleCode) match {
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

    def guessAsServed(name: String) = {
      // logger.info(s"Trying to guess as served set from $name")
      asServedSets.find(set => !set.description.contains("leftover") && set.images.exists(_.url.contains(name))).map(_.id) match {
        case Some(set) => {
          // logger.info(s"Guessed as $set")
          Some(set)
        }
        case None => {
          // logger.info(s"No appropriate set found")
          None
        }
      }
    }

    val (methods, badRefs) = psmTable.foldLeft((Map[String, Seq[PortionSizeMethod]](), Seq[String]())) {
      case ((map, badRefs), (foodCode, psmRefs)) =>

        var r = Seq[String]()

        val psm = psmRefs.map {
          ref =>

            val result = psmIndex.guide.get(ref).orElse {
              guessAsServed(ref).flatMap(set => psmIndex.asServed.get(set))
            }

            if (result.isEmpty) {
              // logger.warn(s"$ref is not a known guide or as served image id, ignoring")
              r +:= ref
            }

            result
        }.flatten

        (map + (foodCode -> psm), badRefs ++ r)
    }
    
    methods*/
  }
}