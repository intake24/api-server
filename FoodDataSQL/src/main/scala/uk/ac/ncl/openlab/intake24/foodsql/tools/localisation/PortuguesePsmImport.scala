package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConverters._
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService
import uk.ac.ncl.openlab.intake24.services.IndexFoodDataService

import org.slf4j.LoggerFactory
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.foodsql.AdminFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.IndexFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseConnection

object PortuguesePsmImport extends App with WarningMessage with DatabaseConnection {

  val logger = LoggerFactory.getLogger(PortuguesePsmImport.getClass)

  val referenceLocaleId = "en_GB"

  trait Options extends ScallopConf {
    version("Intake24 Portuguese PSM data import tool 16.8")

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()
  
  // displayWarningMessage("WARNING: This will override existing portion size methods for Portuguese locale")

  val dataSource = getDataSource(options)

  val dataService = new AdminFoodDataServiceSqlImpl(dataSource)

  val indexDataService = new IndexFoodDataServiceSqlImpl(dataSource)

  logger.info("Retrieving guide image headers")
  
  val guideImages = dataService.allGuideImages().map(_.id).toSet

  logger.info("Retrieving as served set headers")
  val asServedSets = dataService.allAsServedSets().map(_.id).toSet

  logger.info("Building PSM index")

  case class PsmIndex(guide: Map[String, PortionSizeMethod], asServed: Map[String, PortionSizeMethod])

  val psmIndex = indexDataService.indexableFoods(referenceLocaleId).foldLeft(PsmIndex(Map(), Map())) {
    (index, header) =>
      logger.info(s"Processing ${header.toString}")

      dataService.foodRecord(header.code, "en_GB") match {
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

  val reader = new CSVReader(new FileReader(options.csvPath()))

  val z = Map[String, Set[String]]()

  val references = reader.readAll().asScala.foldLeft(z) {
    (acc, row) =>

      val code = row.head
      val psmRefs = row.tail.filterNot(_.isEmpty())

      val set = acc.get(code).getOrElse(Set()) ++ psmRefs
      acc + (code -> set)
  }
  
  reader.close()

  val methods = references.mapValues {
    _.toSeq.map {
      ref =>
        if (guideImages.contains(ref)) psmIndex.guide.get(ref) match {
          case Some(psm) => psm
          case None => throw new RuntimeException(s"Index has no entry for guide image $ref")
        }
        else if (asServedSets.contains(ref)) psmIndex.asServed.get(ref) match {
          case Some(psm) => psm
          case None => throw new RuntimeException(s"Index has no entry for as served image $ref")
        }
        else
          throw new RuntimeException(s"$ref is neither a guide nor as served image id")
    }
  }
  
  methods.keySet.foreach {
    key => 
      logger.info(s"Updating $key")
      
      dataService.foodRecord(key, "pt_PT") match {
        case Right(record) => {
          val updatedLocal = record.local.copy(portionSize = methods(key))
          dataService.updateFoodLocal(key, "pt_PT", updatedLocal)
        }
        case _ => throw new RuntimeException(s"Couldn't retrieve record for $key")
      }
  }
}