package uk.ac.ncl.openlab.intake24.sql.tools.system.migrations

import java.io.FileReader

import anorm.{SqlParser, _}
import au.com.bytecode.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, MigrationRunner}

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls

object SystemV2_CreateMasterNutrientsList extends App with MigrationRunner {

  val options = new ScallopConf(args) with DatabaseConfigurationOptions {
    val nutrientsList = opt[String](required = true)
  }

  options.verify()

  private case class NutrientRow(id: Long, description: String, unit: Long)

  runMigration(2l, 3l, options) {
    implicit connection =>
      val unitIds = SQL("SELECT id, symbol FROM nutrient_units").executeQuery().as((SqlParser.long("id") ~ SqlParser.str("symbol")).*).map {
        case id ~ symbol => symbol -> id
      }.toMap

      println("Parsing nutrient list CSV...")

      val reader = new CSVReader(new FileReader(options.nutrientsList()))

      val lines = reader.readAll().asScala.toIndexedSeq

      reader.close()

      val nutrientNames = lines(0).tail
      val units = lines(1).tail.map(s => unitIds(s))

      println("Deleting existing nutrient types...")

      SQL("DELETE FROM nutrient_types").execute()

      val unitParams = nutrientNames.zip(units).zipWithIndex.map {
        case ((description, unit), index) =>
          Seq[NamedParameter]('id -> (index + 1), 'description -> description, 'unit_id -> unit)
      }

      println("Creating new nutrient types...")

      BatchSql("INSERT INTO nutrient_types VALUES({id},{description},{unit_id})", unitParams.head, unitParams.tail: _*).execute()
  }
}
