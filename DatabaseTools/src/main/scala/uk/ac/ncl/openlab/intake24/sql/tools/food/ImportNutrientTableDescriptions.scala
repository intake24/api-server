package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.FileReader

import au.com.bytecode.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminImpl
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigChooser, DatabaseConnection, WarningMessage}

import scala.collection.JavaConversions._

/**
  * Created by Tim Osadchiy on 22/05/2017.
  */

object ImportNutrientTableDescriptions extends App with DatabaseConnection with WarningMessage {

  val queue = Seq(
    ParseParams("NDNS", "NDNS", 1, 0, 1, None),
    ParseParams("NZ", "NZ", 2, 0, 2, None),
    ParseParams("PT_INSA", "PT_INSA", 3, 0, 3, Some(2)),
    ParseParams("DK_DTU", "DK_DTU", 2, 2, 1, Some(0))
  )

  val options = getOptions()

  case class ParseParams(nutrientTableId: String, csvName: String, rowOffset: Int, idCol: Int, descriptionCol: Int, localDescriptionCol: Option[Int])

  private def getOptions() = {
    val options = new ScallopConf(args) {
      val dbConfigDir = opt[String](required = true)
      val csvDir = opt[String](required = true)
    }
    options.verify()
    options
  }

  private def getCsvRows(csvName: String) = {
    val filePath = s"${options.csvDir()}/${csvName}.csv"
    new CSVReader(new FileReader(filePath)).readAll().toSeq.map(_.toIndexedSeq)
  }

  private def getNutrients(params: ParseParams): Seq[NutrientTableRecord] = {
    getCsvRows(params.csvName).drop(params.rowOffset)
      .map(r => NutrientTableRecord(r.get(params.idCol), params.nutrientTableId, r.get(params.descriptionCol), params.localDescriptionCol.map(i => r.get(i))))
  }

  private def init() = {
    val rows = queue.flatMap(getNutrients)
    recordToDb(rows)
  }

  private def recordToDb(nutrients: Seq[NutrientTableRecord]) = {
    val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
    val dataSource = getDataSource(databaseConfig)
    val nutrientTableService = new NutrientTablesAdminImpl(dataSource)

    nutrientTableService.updateNutrientTableRecordDescriptions(nutrients) match {
      case Right(()) => ()
      case Left(e) => throw e.exception
    }

  }

  init()

}
