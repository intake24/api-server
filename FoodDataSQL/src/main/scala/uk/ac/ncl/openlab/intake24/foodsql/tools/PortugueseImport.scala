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

object PortugueseImport extends App {
  
  val locale_code = "pt_PT"

  case class Options(arguments: Seq[String]) extends ScallopConf(arguments) {
    version("Intake24 Portuguese localisation data import tool 16.7")

    val csvPath = opt[String](required = true, noshort = true)

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

  val dataSource = new org.postgresql.ds.PGSimpleDataSource()

  dataSource.setServerName(opts.pgHost())
  dataSource.setDatabaseName(opts.pgDatabase())
  dataSource.setUser(opts.pgUser())

  opts.pgPassword.foreach(pw => dataSource.setPassword(pw))
  opts.pgUseSsl.foreach(ssl => dataSource.setSsl(ssl))

  val dataService = new AdminFoodDataServiceSqlImpl(dataSource)

  println("Applying do not use flags")
  
  val doNotUseReader = new CSVReader(new FileReader(opts.csvPath() + "/do_not_use.csv"))

  val doNotUseCodes = doNotUseReader.readAll().tail.map(_(0))
  
  doNotUseCodes.foreach {
    code =>
     // println(s"Updating $code")
      dataService.foodDef(code, locale_code) match {
        case Right(fooddef) => dataService.updateFoodLocal(code, locale_code, fooddef.localData.copy(do_not_use = true))
        case Left(CodeError.UndefinedCode) => println(s"Unexpected food code: $code")
      }
  }
  
  doNotUseReader.close()
  
  println("Adding translations to foods using UK nutrient database")
  
  val useUkReader = new CSVReader(new FileReader(opts.csvPath() + "/use_uk.csv"))

  val useUkRows = useUkReader.readAll().tail
    
  useUkRows.foreach {
    row =>
      
      val code = row(0)
      val portugueseName = row(8)
      
     // println(s"Updating $code")
      
      dataService.foodDef(code, locale_code) match {
        case Right(fooddef) => dataService.updateFoodLocal(code, locale_code, fooddef.localData.copy(localDescription = Some(portugueseName)))
        case Left(CodeError.UndefinedCode) => println(s"Unexpected food code: $code")
      }      
  }
}
