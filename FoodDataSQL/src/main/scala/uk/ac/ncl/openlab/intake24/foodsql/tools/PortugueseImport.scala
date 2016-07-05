package uk.ac.ncl.openlab.intake24.foodsql.tools

import org.rogach.scallop.ScallopOption
import org.rogach.scallop.ScallopConf
import java.io.BufferedReader
import java.io.InputStreamReader
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import uk.ac.ncl.openlab.intake24.foodsql.AdminFoodDataServiceSqlImpl
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import uk.ac.ncl.openlab.intake24.services.CodeError
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import uk.ac.ncl.openlab.intake24.foodsql.IndexFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.LocaleManagementSqlImpl
import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.foodsql.NutrientTableManagementSqlImpl
import uk.ac.ncl.openlab.intake24.NutrientTable
import java.util.Properties
import java.io.PrintWriter
import com.zaxxer.hikari.HikariConfig

object PortugueseImport extends App {

  val portugueseLocaleCode = "pt_PT"
  val baseLocaleCode = "en_GB"
  val portugueseNutrientTableCode = "PT"

  case class Options(arguments: Seq[String]) extends ScallopConf(arguments) {
    version("Intake24 Portuguese localisation data import tool 16.7")

    val csvPath = opt[String](required = true, noshort = true)
    val logPath = opt[String](required = false, noshort = true)

    val pgHost = opt[String](required = true, noshort = true)
    val pgDatabase = opt[String](required = true, noshort = true)
    val pgUser = opt[String](required = true, noshort = true)
    val pgPassword = opt[String](noshort = true)
    val pgUseSsl = opt[Boolean](noshort = true)
  }

  val opts = Options(args)

  println("""|======================================================
             |WARNING: THIS WILL OVERWRITE CURRENT PORTUGUESE DATA!
             |======================================================
             |""".stripMargin)

  var proceed = false;

  val reader = new BufferedReader(new InputStreamReader(System.in))

  while (!proceed) {
    println("Are you sure you wish to continue? Type 'yes' to proceed, or press Control-C to exit.")
    val input = reader.readLine()
    if (input == "yes") proceed = true;
    if (input == "no") System.exit(0);
  }

  val logger = LoggerFactory.getLogger(getClass)

  DriverManager.registerDriver(new org.postgresql.Driver)

  val dbConnectionProps = new Properties();
  dbConnectionProps.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
  dbConnectionProps.setProperty("dataSource.user", opts.pgUser())
  dbConnectionProps.setProperty("dataSource.databaseName", opts.pgDatabase())
  dbConnectionProps.setProperty("dataSource.serverName", opts.pgHost())
  dbConnectionProps.put("dataSource.logWriter", new PrintWriter(System.out))

  opts.pgPassword.foreach(pw => dbConnectionProps.setProperty("dataSource.password", pw))
  opts.pgUseSsl.foreach(ssl => dbConnectionProps.setProperty("dataSource.ssl", ssl.toString()))

  val dataSource = new HikariDataSource(new HikariConfig(dbConnectionProps))

  val dataService = new AdminFoodDataServiceSqlImpl(dataSource)

  val indexDataService = new IndexFoodDataServiceSqlImpl(dataSource)

  // Should be an insert-update loop, but this is just a one-time script so using unsafe way
  val localeService = new LocaleManagementSqlImpl(dataSource)
  localeService.delete(portugueseLocaleCode)
  localeService.create(Locale(portugueseLocaleCode, "Portuguese (Portugal)", "Português", portugueseLocaleCode, "pt", "pt", Some(baseLocaleCode)))

  val nutrientTableService = new NutrientTableManagementSqlImpl(dataSource)
  nutrientTableService.delete(portugueseNutrientTableCode)
  nutrientTableService.create(NutrientTable(portugueseNutrientTableCode, "Portuguese food composition database"))

  val doNotUseReader = new CSVReader(new FileReader(opts.csvPath() + "/do_not_use.csv"))

  val doNotUseCodes = doNotUseReader.readAll().tail.map(_(0)).toSet

  doNotUseReader.close()

  val useUkReader = new CSVReader(new FileReader(opts.csvPath() + "/use_uk.csv"))

  val useUk = useUkReader.readAll().tail.map(row => row(0) -> row(9)).toMap

  useUkReader.close()

  val usePtReader = new CSVReader(new FileReader(opts.csvPath() + "/use_pt.csv"))

  val usePt = usePtReader.readAll().tail.map(row => row(0) -> (row(7), row(9))).toMap

  usePtReader.close()

  val indexableFoods = indexDataService.indexableFoods(baseLocaleCode)
  
  indexableFoods.foreach {
    header =>
      print(s""""${header.code}", "${header.localDescription}"""")

      dataService.foodRecord(header.code, portugueseLocaleCode) match {
        case Right(fooddef) => {
          val localData = fooddef.localData

          if (doNotUseCodes.contains(header.code)) {
            println(s""","Not using in Portuguese locale"""")
            dataService.updateFoodLocal(header.code, portugueseLocaleCode, localData.copy(doNotUse = true))
          } else useUk.get(header.code) match {
            case Some(portugueseDescription) => {
              println(s""","Inheriting UK food database code", "$portugueseDescription"""")
              dataService.updateFoodLocal(header.code, portugueseLocaleCode, localData.copy(localDescription = Some(portugueseDescription), doNotUse = false))
            }
            case None => usePt.get(header.code) match {
              case Some((portugueseCode, portugueseDescription)) => {
                println(s""","Using Portuguese food database code", "$portugueseDescription", "$portugueseCode"""")
                dataService.updateFoodLocal(header.code, portugueseLocaleCode, localData.copy(localDescription = Some(portugueseDescription), nutrientTableCodes = Map(portugueseNutrientTableCode -> portugueseCode), doNotUse = false))
              }
              case None => {
                println(""","Not in Portuguese recoding tables!"""")
              }
            }
          }
        }
        case _ => throw new RuntimeException(s"Could not retrieve food definition for ${header.localDescription} (${header.code})")
      }
  }
}
