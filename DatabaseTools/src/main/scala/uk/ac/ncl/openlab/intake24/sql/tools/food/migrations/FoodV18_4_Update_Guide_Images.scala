package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools._

import anorm.SQL

object FoodV18_4_Update_Guide_Images extends App with MigrationRunner with WarningMessage {

  trait Options extends ScallopConf with DatabaseConfigurationOptions

  val versionFrom = 18l

  val options = new ScallopConf(args) with Options

  options.verify()

  runMigration(18, 19, options) {
    implicit conn =>

      SQL("UPDATE guide_images SET image_map_id=id").executeUpdate()
      SQL("DELETE FROM guide_image_objects WHERE guide_image_id='Gcri' AND object_id > 6")
      SQL("DELETE FROM guide_image_objects WHERE guide_image_id='Gyog' AND object_id > 12")
      SQL("UPDATE guide_image_objects SET image_map_id=guide_image_id,image_map_object_id=object_id")

  }
}