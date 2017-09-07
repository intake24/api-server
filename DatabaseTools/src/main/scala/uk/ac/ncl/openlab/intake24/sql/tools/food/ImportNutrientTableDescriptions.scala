package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.FileReader

import anorm.{SQL, SqlParser}
import au.com.bytecode.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminImpl
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls


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

  val options = new ScallopConf(args) with DatabaseConfigurationOptions {
    val csvDir = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
  val dataSource = getDataSource(databaseConfig)
  val nutrientTableService = new NutrientTablesAdminImpl(dataSource)

  case class ParseParams(nutrientTableId: String, csvName: String, rowOffset: Int, idCol: Int, descriptionCol: Int, localDescriptionCol: Option[Int])

  private def getCsvRows(csvName: String) = {
    val filePath = s"${options.csvDir()}/${csvName}.csv"
    new CSVReader(new FileReader(filePath)).readAll().asScala.map(_.toIndexedSeq)
  }

  private def getNutrients(params: ParseParams): Seq[NutrientTableRecord] = {
    getCsvRows(params.csvName).drop(params.rowOffset)
      .map(r => NutrientTableRecord(r(params.idCol), params.nutrientTableId, r(params.descriptionCol), params.localDescriptionCol.map(i => r(i))))
  }


  val rows = queue.flatMap(getNutrients)

  // Ugly hack but easier than adapting MigrationRunner that expects only raw database operations (without services)
  // Using services such as NutrientTableService in migrations is generally a bad idea because they will change
  // and the migration will stop compiling in that case.

  val versionFrom = 36
  val versionTo = 37

  val conn1 = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery()(conn1).as(SqlParser.long("version").single)(conn1)

  conn1.close()

  if (version != versionFrom) {
    throw new RuntimeException(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    nutrientTableService.updateNutrientTableRecordDescriptions(rows) match {
      case Right(()) => ()
      case Left(e) => throw e.exception
    }


    println("Updating schema version...")

    val conn2 = dataSource.getConnection

    SQL("UPDATE schema_version SET version={version_to} WHERE version={version_from}").on('version_from -> versionFrom, 'version_to -> versionTo)
      .execute()(conn2)

    conn2.close()
  }
}
