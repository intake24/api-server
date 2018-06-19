package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.FileReader

import anorm.{BatchSql, NamedParameter, SQL, SqlParser}
import au.com.bytecode.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser.excelColumnToOffset
import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableParser, LegacyNutrientTables}
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls


object ImportNdnsCompoundGroups extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) with DatabaseConfigurationOptions {
    val csv = opt[String](required = true)
  }

  options.verify()

  val rowOffset = 1
  val colRange = excelColumnToOffset("C").to(excelColumnToOffset("AY"))

  val csvReader = new CSVReader(new FileReader(options.csv()))

  val rows = csvReader.readAll().asScala.drop(rowOffset)

  val params = rows.toSeq.flatMap {
    row =>
      colRange.map {
        index =>
          Seq(NamedParameter("ndns_food_code", row(0).toInt),
            NamedParameter("compound_food_group_id", (index - colRange.start) + 1),
            NamedParameter("proportion", row(index - 1).toDouble))
      }
  }

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
  val dataSource = getDataSource(databaseConfig)

  val conn1 = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery()(conn1).as(SqlParser.long("version").single)(conn1)

  if (version != 49) {
    throw new RuntimeException(s"Wrong schema version: expected 49, got $version")
  } else {
    BatchSql("INSERT INTO ndns_compound_food_groups_data VALUES({ndns_food_code},{compound_food_group_id},{proportion})", params.head, params.tail:_*).execute()(conn1)
  }

  conn1.close()
}
