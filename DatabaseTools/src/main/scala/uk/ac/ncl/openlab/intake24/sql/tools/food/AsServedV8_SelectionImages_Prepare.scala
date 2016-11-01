package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate

import scala.collection.mutable.Buffer

import org.apache.commons.io.FilenameUtils
import org.rogach.scallop.ScallopConf
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import anorm.Macro
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import upickle.default._

import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import java.util.UUID

import scala.collection.JavaConverters._
import java.io.File
import anorm.NamedParameter
import anorm.BatchSql
import anorm.AnormUtil

object AsServedV8_SelectionImages_Apply extends App with WarningMessage with DatabaseConnection {

  trait Options extends ScallopConf {
    version("Intake24 v8 as served apply")

    val remappingFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 7l
  val versionTo = 8l

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

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