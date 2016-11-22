package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import anorm.{BatchSql, NamedParameter, SqlParser, _}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConnection, DatabaseOptions, WarningMessage}
import upickle.default._

/**
  * Created by nip13 on 22/11/2016.
  */
object FoodV10_SourceImageThumbnails_Apply extends App with WarningMessage with DatabaseConnection {

  trait Options extends ScallopConf {
    version("Intake24 food database generate source image thumbnails")

    val imageDir = opt[String](required = true, noshort = true)

    val remappingFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 10l
  val versionTo = 11l

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  val dataSource = getDataSource(options)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    println(s"Wrong schema version: expected $versionFrom, got $version")
  } else {
    val imageMap = read[Map[String,String]](scala.io.Source.fromFile(options.remappingFile()).getLines().mkString).toSeq

    val params = imageMap.map {
      case (srcPath, dstPath) =>
        Seq[NamedParameter]('path -> srcPath, 'thumbnail_path -> dstPath)
    }

    dbConn.setAutoCommit(false)

    println("Writing thumbnail paths...")

    val query = "UPDATE source_images SET thumbnail_path={thumbnail_path} WHERE path={path}"

    BatchSql(query, params.head, params.tail:_*).execute()

    println("Updating schema version...")

    SQL("UPDATE schema_version SET version={version_to} WHERE version={version_from}").on('version_from -> versionFrom, 'version_to -> versionTo).execute()

    println("Done!")

    dbConn.commit()
  }
}
