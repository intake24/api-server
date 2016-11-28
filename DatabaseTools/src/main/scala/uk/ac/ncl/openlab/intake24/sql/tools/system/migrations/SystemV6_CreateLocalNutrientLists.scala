package uk.ac.ncl.openlab.intake24.sql.tools.system.migrations

import java.io.FileReader

import anorm.{SqlParser, _}
import au.com.bytecode.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, MigrationRunner}

import scala.collection.JavaConverters._

object SystemV6_CreateLocalNutrientLists extends App with MigrationRunner {

  val options = new ScallopConf(args) with DatabaseConfigurationOptions {
    val nutrientsList = opt[String](required = true)
  }

  options.verify()

  private case class NutrientRow(id: Long, description: String, unit: Long)

  runMigration(6l, 7l, options) {
    implicit connection =>
      val unitIds = SQL("SELECT id, symbol FROM nutrient_units").executeQuery().as((SqlParser.long("id") ~ SqlParser.str("symbol")).*).map {
        case id ~ symbol => symbol -> id
      }.toMap

      println("Parsing nutrient list CSV...")

      val reader = new CSVReader(new FileReader(options.nutrientsList()))

      val lines = reader.readAll().asScala.toIndexedSeq

      reader.close()

      val descriptions = lines(0).tail.map(_.trim)

      val inUKTable = lines(2).tail.map(_.trim).zipWithIndex.foldLeft(Set[Long]()) {
        case (acc, (col, index)) => if (col.isEmpty) acc else (acc + (index + 1).toLong)
      }

      val inPTTable = lines(3).tail.map(_.trim).zipWithIndex.foldLeft(Set[Long]()) {
        case (acc, (col, index)) => if (col.isEmpty) acc else (acc + (index + 1).toLong)
      }

      val inNZTable = lines(4).tail.map(_.trim).zipWithIndex.foldLeft(Set[Long]()) {
        case (acc, (col, index)) => if (col.isEmpty) acc else (acc + (index + 1).toLong)
      }

      val inDKTable = lines(5).tail.map(_.trim).zipWithIndex.foldLeft(Set[Long]()) {
        case (acc, (col, index)) => if (col.isEmpty) acc else (acc + (index + 1).toLong)
      }

      /*  Energy (Kcal)
      Fat
      Saturated Fat
      Protein
      Carbohydrate
      Total Sugars
      NMES
      Alcohol
      Fibre
      Vitamin C
      Calcium
      Iron */

      val coreNutrients = Seq(1l, 49l, 50l, 11l, 13l, 22l, 23l, 20l, 18l, 19l, 129l, 140l, 143l)

      val ukLocalTypes = coreNutrients ++ (inUKTable.toSeq.sortBy(id => descriptions(id.toInt - 1).toLowerCase()).filterNot(coreNutrients.contains))
      val ptLocalTypes = coreNutrients ++ ((inUKTable ++ inPTTable).toSeq.sortBy(id => descriptions(id.toInt - 1).toLowerCase()).filterNot(coreNutrients.contains))
      val nzLocalTypes = coreNutrients ++ ((inUKTable ++ inNZTable).toSeq.sortBy(id => descriptions(id.toInt - 1).toLowerCase()).filterNot(coreNutrients.contains))
      val dkLocalTypes = coreNutrients ++ ((inUKTable ++ inDKTable).toSeq.sortBy(id => descriptions(id.toInt - 1).toLowerCase()).filterNot(coreNutrients.contains))

      SQL("DELETE FROM local_nutrient_types").execute()

      def createTypes(types: Seq[Long], locale: String) = {
        println(s"Creating local nutrient types for $locale...")

        val query = "INSERT INTO local_nutrient_types VALUES(DEFAULT, {locale_id}, {nutrient_type_id})"

        val params = types.map {
          typeId => Seq[NamedParameter]('locale_id -> locale, 'nutrient_type_id -> typeId)
        }

        BatchSql(query, params.head, params.tail: _*).execute()
      }

      createTypes(ukLocalTypes, "en_GB")
      createTypes(ptLocalTypes, "pt_PT")
      createTypes(nzLocalTypes, "en_NZ")
      createTypes(dkLocalTypes, "da_DK")
  }
}
