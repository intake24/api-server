/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.datastoresql.tools

import anorm._
import org.rogach.scallop._
import java.sql.DriverManager
import scala.xml.XML
import java.io.File
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import java.sql.Connection
import scala.collection.JavaConversions._
import java.sql.Timestamp
import org.workcraft.gwt.shared.client.Callback1
import net.scran24.datastore.NutritionMappedSurveyRecord
import scala.collection.mutable.Buffer
import java.util.UUID
import uk.ac.ncl.openlab.intake24.datastoresql.Queries
import uk.ac.ncl.openlab.intake24.datastoresql.Util._
import uk.ac.ncl.openlab.intake24.datastoresql.JavaConversions._
import net.scran24.datastore.NutritionMappedSurveyRecordWithId
import java.util.Properties
import uk.ac.ncl.openlab.intake24.datastoresql.JavaConversions
import java.io.BufferedReader
import java.io.InputStreamReader

import uk.ac.ncl.openlab.intake24.commonsqlresql.Util.*;
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreSqlImpl
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.datastoresql.SecureUserRecord

case class InitOptions(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("Intake24 SQL system database setup tool 16.1-SNAPSHOT")

  val pgHost = opt[String](required = true, noshort = true)
  val pgDatabase = opt[String](required = true, noshort = true)
  val pgUser = opt[String](required = true, noshort = true)
  val pgPassword = opt[String](noshort = true)
  val pgUseSsl = opt[Boolean](noshort = true)
}

object Init extends App {

  val opts = InitOptions(args)

  println("""|=============================================================
              |WARNING: THIS WILL DESTROY ALL EXISTING DATA IN THE DATABASE!
              |=============================================================
              |""".stripMargin)

  var proceed = false;

  val reader = new BufferedReader(new InputStreamReader(System.in))
  
  while (!proceed) {
    println("Are you sure you wish to continue? Type 'yes' to proceed, type 'no' or hit Control-C to exit.")    
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
  
  implicit val dbConn = dataSource.getConnection

  def separateSqlStatements(sql: String) =
    // Regex matches on semicolons that neither precede nor follow other semicolons
    sql.split("(?<!;);(?!;)").map(_.trim.replace(";;", ";")).filterNot(_.isEmpty)

  def stripComments(s: String) = """(?m)/\*(\*(?!/)|[^*])*\*/""".r.replaceAllIn(s, "")
    
  val initDbStatements = separateSqlStatements(stripComments(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/sql/init_system_db.sql"), "utf-8").mkString))

  logger.info("Dropping all tables and sequences")

  val dropTableStatements =
    SQL("""SELECT 'DROP TABLE IF EXISTS ' || tablename || ' CASCADE;' AS query FROM pg_tables WHERE schemaname='public'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val dropSequenceStatements =
    SQL("""SELECT 'DROP SEQUENCE IF EXISTS ' || relname || ' CASCADE;' AS query FROM pg_class WHERE relkind = 'S'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val clearDbStatements = dropTableStatements ++ dropSequenceStatements
  
  clearDbStatements.foreach {
    statement =>
      logger.debug(statement)
      SQL(statement).execute()
  }

  logger.info("Creating tables")

  initDbStatements.foreach { statement =>
    logger.debug(statement)
    SQL(statement).execute()
  }

  val sqlDataStore = new DataStoreSqlImpl(dataSource)

  // add default admin user 
  // admin/intake24
  
  logger.info("Creating the default admin user")
  
  sqlDataStore.addUser("", SecureUserRecord("admin", "7klnEraBssvRBTnFR5FbIJ/5Qjqgf8w3/7Rs4gBoFBY=", "hUkIQLASWraVS4JDPOr8tA==", "shiro-sha256", Set("superuser", "admin"), Set(), Map()))
}
