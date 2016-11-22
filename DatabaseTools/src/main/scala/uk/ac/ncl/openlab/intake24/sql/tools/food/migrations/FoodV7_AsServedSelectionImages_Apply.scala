package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import anorm.{AnormUtil, BatchSql, NamedParameter, SqlParser, _}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConnection, DatabaseOptions, WarningMessage}
import upickle.default._

/**
  * Created by nip13 on 22/11/2016.
  */
object FoodV7_AsServedSelectionImages_Apply extends App with WarningMessage with DatabaseConnection {

  trait Options extends ScallopConf {
    version("Intake24 v8 as served apply")

    val remappingFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 7l
  val versionTo = 8l

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  val dataSource = getDataSource(options)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

    if (version != versionFrom) {
    println(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    val images = read[Map[String, ImagePaths]](scala.io.Source.fromFile(options.remappingFile()).getLines().mkString).toSeq

    val params = images.map {
      case (_, ImagePaths(sourcePath, processedPath)) =>
        Seq[NamedParameter]('processed_path -> processedPath, 'source_path -> sourcePath)
    }

    dbConn.setAutoCommit(false)

    println("Creating processed image records...")

    val query = "INSERT INTO processed_images VALUES (DEFAULT,{processed_path},(SELECT id FROM source_images WHERE path={source_path}),3,DEFAULT)"

    val processedKeys = AnormUtil.batchKeys(BatchSql(query, params.head, params.tail:_*))

    val keysParams = images.map(_._1).zip(processedKeys).map {
      case (setId, key) =>
        Seq[NamedParameter]('set_id -> setId, 'image_id -> key)
    }

    println("Setting selection image ids for as served sets...")

    BatchSql("UPDATE as_served_sets SET selection_image_id={image_id} WHERE id={set_id}", keysParams.head, keysParams.tail:_*).execute()

    println("Updating schema version...")

    SQL("UPDATE schema_version SET version={version_to} WHERE version={version_from}").on('version_from -> versionFrom, 'version_to -> versionTo).execute()

    println("Done!")

    dbConn.commit()

  }
}
