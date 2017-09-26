package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import java.io.FileReader

import com.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.SingleNutrientTypeUpdate
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, ErrorHandler, WarningMessage}

import scala.collection.JavaConverters._

object CO2DataImport extends App with WarningMessage with DatabaseConnection with ErrorHandler {

  trait Options extends DatabaseConfigurationOptions {

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  displayWarningMessage(s"This will update NDNS nutrient records to include CO2 emissions data in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val nutrientTableService = new NutrientTablesAdminImpl(dataSource)


  val knownNdnsRecords = throwOnError(nutrientTableService.getNutrientTableRecordIds("NDNS")).toSet

  val reader = new CSVReader(new FileReader(options.csvPath()))

  val csvRows = reader.readAll().asScala.zipWithIndex.tail

  val updates = scala.collection.mutable.Buffer[SingleNutrientTypeUpdate]()

  val unknownRecordIds = scala.collection.mutable.Buffer[String]()

  val badRowIds = scala.collection.mutable.Buffer[Int]()

  csvRows.foreach {
    case (row, index) =>
      val recordId = row(0).trim

      if (knownNdnsRecords.contains(recordId)) {

        try {
          val value = row(3).toDouble
          updates.append(SingleNutrientTypeUpdate(recordId, Some(value)))
        } catch {
          case e: NumberFormatException =>
            badRowIds.append(index + 1)
        }

      } else {
        unknownRecordIds.append(recordId)
      }
  }

  println(s"These rows contain empty/invald values: ${badRowIds.mkString(", ")}")
  println(s"These NDNS codes are not in Intake24 database: ${unknownRecordIds.mkString(", ")}")

  throwOnError(nutrientTableService.updateSingleNutrientType("NDNS", 228, updates))
}
