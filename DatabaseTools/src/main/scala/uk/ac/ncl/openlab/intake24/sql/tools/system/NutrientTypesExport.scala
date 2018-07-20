package uk.ac.ncl.openlab.intake24.sql.tools.system

import anorm._
import io.circe.generic.auto._
import io.circe.syntax._
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.language.reflectiveCalls

object NutrientTypesExport extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
    val locale = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())

  val dataSource = getDataSource(databaseConfig)

  implicit val connection = dataSource.getConnection

  case class LocalNutrient(id: Long, name: String, unit: String)

  case class LocalNutrientString(id: String, name: String, unit: String)

  val fixed =
    """
      |SELECT
      |  t.id,
      |  n.description    AS name,
      |  nu.symbol        AS unit
      |FROM (VALUES(1, 1),
      |(11, 2),
      |(49, 3),
      |(13, 4),
      |(20, 5),
      |(22, 6),
      |(50, 7),
      |(59, 8),
      |(120, 9),
      |(122, 10),
      |(129, 11),
      |(130, 12),
      |(138, 13),
      |(140, 14),
      |(143, 15)
      |) AS t(id, ord)
      |JOIN nutrient_types n ON t.id = n.id
      |JOIN nutrient_units nu ON n.unit_id = nu.id
      |ORDER BY t.ord
    """.stripMargin

  val local = "select nutrient_type_id as id, n.description as name, nu.symbol as unit from local_nutrient_types join nutrient_types n ON local_nutrient_types.nutrient_type_id = n.id JOIN nutrient_units nu ON n.unit_id = nu.id WHERE locale_id='en_GB' ORDER BY local_nutrient_types.id"

  val nutrients = SQL(local)
    .executeQuery()
    .as(Macro.namedParser[LocalNutrient].*)

  val out = Map(options.locale() -> nutrients.map(n => LocalNutrientString(n.id.toString, n.name, n.unit)))

  println(out.asJson.spaces4)
}
