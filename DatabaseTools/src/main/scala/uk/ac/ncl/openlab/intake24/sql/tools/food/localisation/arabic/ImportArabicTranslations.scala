package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.arabic

import java.io.FileReader

import com.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.LocalFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.api.client.{ApiConfigChooser, ApiConfigurationOptions, Intake24ApiClient}
import uk.ac.ncl.openlab.intake24.api.shared.EmailCredentials

import scala.collection.JavaConverters._

/**
  * This is simpler than Danish and Portuguese localisation because only Arabic food names need to be imported,
  * new foods will be added via food database admin tool
  */

object ImportArabicTranslations extends App {

  val logger = LoggerFactory.getLogger(getClass)

  val baseLocaleCode = "en_GB"

  val arabicLocaleCode = "ar_AE"

  val keepCellColors = Seq("F2DCDB", "E6B8B7")

  val deleteCellColors = Seq("C0504D", "963634")

  val cellColorIndex = 0

  val foodCodeIndex = 1

  val arabicNameIndex = 3

  class Options extends ScallopConf(args) with ApiConfigurationOptions {
    version("Intake24 Arabic localisation import tool")

    val recodingTablePath = opt[String](required = true, noshort = true)
  }

  val options = new Options()

  options.verify()

  def parseCSV(csvPath: String): (Map[String, String], Seq[String]) = {
    val reader = new CSVReader(new FileReader(csvPath))

    val result = reader.readAll().asScala.foldLeft((Map[String, String](), Seq[String]())) {
      case ((translated, hidden), row) =>
        if (keepCellColors.contains(row(cellColorIndex))) {
          val foodCode = row(foodCodeIndex)
          val arabicName = row(arabicNameIndex).replace('8', '"')

          (translated + (foodCode -> arabicName), hidden)
        } else if (deleteCellColors.contains(row(cellColorIndex)))
          (translated, row(foodCodeIndex) +: hidden)
        else {
          println (s"Unrecognised cell colour: ${row(foodCodeIndex)}")
          (translated, hidden)
        }
    }

    reader.close()

    result
  }


  val csv = parseCSV(options.recodingTablePath())


  val apiConfig = ApiConfigChooser.chooseApiConfiguration(options.apiConfigDir())

  val apiClient = new Intake24ApiClient(apiConfig.baseUrl, EmailCredentials(apiConfig.userName, apiConfig.password))

  csv._1.foreach {
    case (code, name) =>
      print(s"Updating name for $code ($name) ... ")

      val result = for (foodRecord <- apiClient.foods.getFoodRecord(code, arabicLocaleCode).right;
                        _ <- apiClient.foods.updateLocalFoodRecord(code, arabicLocaleCode, LocalFoodRecordUpdate(foodRecord.local.version, Some(name),
                          false, Map(), Seq(), Seq(), Seq())).right) yield ()

      result match {
        case Right(()) => println("OK")
        case Left(e) => println(e)
      }
  }

  csv._2.foreach {
    code =>
      print(s"Setting $code as hidden ... ")

      val result = for (foodRecord <- apiClient.foods.getFoodRecord(code, arabicLocaleCode).right;
                        _ <- apiClient.foods.updateLocalFoodRecord(code, arabicLocaleCode, LocalFoodRecordUpdate(foodRecord.local.version, None,
                          true, Map(), Seq(), Seq(), Seq())).right) yield ()

      result match {
        case Right(()) => println("OK")
        case Left(e) => println(e)
      }
  }
}
