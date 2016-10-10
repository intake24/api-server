package uk.ac.ncl.openlab.intake24.foodsql.migrations

import uk.ac.ncl.openlab.intake24.sql.migrations.Migration
import org.slf4j.Logger
import javax.sql.DataSource
import anorm.SQL
import anorm.Macro
import java.nio.file.Path
import java.nio.file.Files
import java.util.function.BiPredicate
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.JavaConverters
import java.nio.file.Paths
import org.apache.commons.io.FilenameUtils
import java.io.File
import uk.ac.ncl.openlab.intake24.sql.migrations.MigrationError
import uk.ac.ncl.openlab.intake24.sql.migrations.MigrationFailed
import anorm.NamedParameter
import anorm.BatchSql
import anorm.AnormUtil

class AsServedToV3 extends Migration {
  val versionFrom = 2l
  val versionTo = 3l
  val description = "Update as_served_images to use processed_images"

  private case class AsServedImageRow(id: Long, as_served_set_id: String, url: String)

  private case class RemappedAsServedImage(id: Long, set_id: String, sourcePath: String, mainImagePath: String, thumbnailPath: String)

  def copySource(id: String, sourceBaseDir: Path, imageBaseDir: Path, imagePath: String, targetDir: Path, logger: Logger): String = {

    val name = FilenameUtils.getName(imagePath)

    logger.info(s"Trying to locate source file for $imagePath")

    val matcher = new BiPredicate[Path, BasicFileAttributes] {
      def test(path: Path, attr: BasicFileAttributes) = path.getFileName().toString().equals(name)
    }

    val file = Files.find(sourceBaseDir, 20, matcher).findFirst()

    val dstRelativePath = s"source/as_served/$id/${FilenameUtils.getName(imagePath)}"
    val dstPath = targetDir.resolve(dstRelativePath)
    dstPath.toFile().getParentFile().mkdirs()

    val sourcePath = if (file.isPresent()) {
      val result = file.get
      logger.info(s"Found high-res source: ${result.toString()}")
      result
    } else {
      val result = imageBaseDir.resolve(imagePath)
      logger.info(s"No high-res source available, using low-res: ${result.toString()}")
      result
    }

    logger.info(s"Copying source image ${sourcePath.toString()} to ${dstPath.toString()}")

    Files.copy(sourcePath, dstPath)
    
    dstRelativePath
  }

  def copyMain(baseDir: Path, targetDir: Path, id: String, imagePath: String, logger: Logger): String = {

    val dstName = s"as_served/$id/${FilenameUtils.getName(imagePath)}"

    val srcPath = baseDir.resolve(imagePath)
    val dstPath = targetDir.resolve(dstName)

    logger.info(s"Copying main image ${srcPath.toString()} to ${dstPath.toString()}")

    dstPath.toFile().getParentFile().mkdirs()

    Files.copy(srcPath, dstPath)

    dstName
  }

  def copyThumb(baseDir: Path, targetDir: Path, id: String, imagePath: String, logger: Logger): String = {

    val dstName = s"as_served/$id/thumbnails/${FilenameUtils.getName(imagePath)}"

    val srcPath = baseDir.resolve(s"Thumbnails/$imagePath")
    val dstPath = targetDir.resolve(dstName)

    logger.info(s"Copying thumbnail image ${srcPath.toString()} to ${dstPath.toString()}")

    dstPath.toFile().getParentFile().mkdirs()

    Files.copy(srcPath, dstPath)

    dstName
  }

  def apply(logger: Logger)(implicit connection: java.sql.Connection): Either[MigrationFailed, Unit] = {
    val imageDir = Option(System.getenv("INTAKE24_IMAGE_DIR")).map(Paths.get(_))
    val sourceImageDir = Option(System.getenv("INTAKE24_SOURCE_IMAGE_DIR")).map(Paths.get(_))
    val targetImageDir = Option(System.getenv("INTAKE24_TARGET_IMAGE_DIR")).map(Paths.get(_))

    if (imageDir.isEmpty || sourceImageDir.isEmpty || targetImageDir.isEmpty)
      Left(MigrationFailed(new RuntimeException("Please set INTAKE24_IMAGE_DIR, INTAKE24_SOURCE_IMAGE_DIR and INTAKE24_TARGET_IMAGE_DIR environment variables to apply this migration")))
    else {
      val rows = SQL("SELECT id, as_served_set_id, url FROM as_served_images ORDER BY id").executeQuery().as(Macro.namedParser[AsServedImageRow].*)

      val remapped = rows.map {
        row =>
          RemappedAsServedImage(row.id, row.as_served_set_id,
            copySource(row.as_served_set_id, sourceImageDir.get, imageDir.get, row.url, targetImageDir.get, logger),
            copyMain(imageDir.get, targetImageDir.get, row.as_served_set_id, row.url, logger),
            copyThumb(imageDir.get, targetImageDir.get, row.as_served_set_id, row.url, logger))
      }

      val availableSource = remapped.map(_.sourcePath)

      val sourceParams = availableSource.map {
        path =>
          Seq[NamedParameter]('path -> path, 'keywords -> "", 'uploader -> "admin#")
      }

      val keys = AnormUtil.batchKeys(BatchSql("INSERT INTO source_images VALUES (DEFAULT,{path},{keywords},{uploader},DEFAULT)", sourceParams))

      val sourceIdMap = availableSource.zip(keys).toMap

      val processedMainParams = remapped.map {
        r =>
          Seq[NamedParameter]('path -> r.mainImagePath, 'source_id -> sourceIdMap(r.sourcePath), 'purpose -> 1l)
      }

      BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedMainParams).execute()

      val processedThumbParams = remapped.map {
        r =>
          Seq[NamedParameter]('path -> r.thumbnailPath, 'source_id -> sourceIdMap(r.sourcePath), 'purpose -> 2l)
      }

      BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedThumbParams).execute()

      Right(())
    }
  }

  def unapply(logger: Logger)(implicit connection: java.sql.Connection): Either[MigrationFailed, Unit] = {
    Left(MigrationFailed(new RuntimeException("This migration is destructive and cannot be unapplied")))
  }
}