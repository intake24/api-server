package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import anorm.{BatchSql, NamedParameter, SQL, SqlParser}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools._

object FoodV18_Guide_Fix_Gcri_Gyog extends App with MigrationRunner with WarningMessage {

  trait Options extends ScallopConf with DatabaseConfigurationOptions

  val options = new ScallopConf(args) with Options

  options.verify()

  val versionFrom = 18l

  val dbConfig = chooseDatabaseConfiguration(options)

  val dataSource = getDataSource(dbConfig)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    throw new RuntimeException(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    SQL("DELETE FROM guide_image_objects WHERE guide_image_id={id}").on('id -> "Gcri").execute()

    val params = Seq(
      Seq[NamedParameter]('object_id -> 1l, 'weight -> 26.0),
      Seq[NamedParameter]('object_id -> 2l, 'weight -> 50.0),
      Seq[NamedParameter]('object_id -> 3l, 'weight -> 34.0),
      Seq[NamedParameter]('object_id -> 4l, 'weight -> 25.0),
      Seq[NamedParameter]('object_id -> 5l, 'weight -> 34.0),
      Seq[NamedParameter]('object_id -> 6l, 'weight -> 25.0)
    )

    BatchSql("INSERT INTO guide_image_objects values (DEFAULT, 'Gcri', {object_id}, 'Crisps', {weight}, NULL, NULL)", params.head, params.tail:_*).execute()

    SQL("DELETE FROM guide_image_objects WHERE guide_image_id='Gyog' and object_id > 12").execute()
  }
}
