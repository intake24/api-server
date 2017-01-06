package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.charset.Charset
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths}
import java.util.function.BiPredicate

import anorm.{Macro, SQL, SqlParser}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools._
import upickle.default.write

import scala.collection.JavaConverters._

case class FoodV18_Guide_Descriptions(legacyImageMapList: Seq[String], objectDescriptions: Map[String, Seq[(Int, String)]])

object FoodV18_1_Dump_Guide_Descriptions extends App with MigrationRunner with WarningMessage {

  trait Options extends ScallopConf with DatabaseConfigurationOptions {
    val compiledImageMapsDir = opt[String](required = true, noshort = true)

    val descFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 18l

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  val dataSource = getDataSource(dbConfig)

  private def getLegacyImageMapList(): Seq[String] = {
    val predicate = new BiPredicate[Path, BasicFileAttributes] {
      override def test(t: Path, u: BasicFileAttributes): Boolean = t.getFileName.toString.endsWith(".imagemap")
    }

    Files.find(Paths.get(options.compiledImageMapsDir()), 1, predicate).iterator().asScala.toSeq.map(_.getFileName.toString.replace(".imagemap", ""))
  }

  private case class GuideImageObjectRow(guide_image_id: String, object_id: Int, description: String)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    throw new RuntimeException(s"Wrong schema version: expected $versionFrom, got $version")
  } else {
    println("Loading legacy image map descriptions")

    val objectDescriptions = SQL("SELECT guide_image_id, object_id, description FROM guide_image_objects").executeQuery().as(Macro.namedParser[GuideImageObjectRow].*).foldLeft(Map[String, Seq[(Int, String)]]()) {
      (acc, row) =>
        val key = row.guide_image_id
        val obj = (row.object_id, row.description)
        acc + (key -> (obj +: acc.get(key).getOrElse(List())))
    }

    println("Loading legacy image map names")

    val imageMapIds = getLegacyImageMapList()

    println("Writing guide image descriptions")

    val writer = Files.newBufferedWriter(Paths.get(options.descFile()), Charset.forName("utf-8"))

    writer.write(write(FoodV18_Guide_Descriptions(imageMapIds, objectDescriptions)))

    writer.close()

  }
}
