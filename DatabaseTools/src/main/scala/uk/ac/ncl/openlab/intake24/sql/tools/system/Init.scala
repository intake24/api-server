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

package uk.ac.ncl.openlab.intake24.sql.tools.system

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

import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreSqlImpl
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.datastoresql.SecureUserRecord
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.SqlFileUtil
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConfiguration

trait InitOptions extends ScallopConf {
  version("Intake24 SQL system database init tool 2.0.0-SNAPSHOT")

  val createAdmin = opt[Boolean](noshort = true)
  val noWarning = opt[Boolean](noshort = true)
}

object InitConsole extends App with WarningMessage {

  val options = new ScallopConf(args) with InitOptions with DatabaseOptions

  options.afterInit()

  if (!options.noWarning())
    displayWarningMessage("WARNING: THIS WILL DESTROY ALL DATA IN THE SYSTEM DATABASE!")

  Init.run(options.databaseConfig, options.createAdmin())
}

object Init extends DatabaseConnection with SqlFileUtil {

  val logger = LoggerFactory.getLogger(getClass)

  def run(databaseConfig: DatabaseConfiguration, createAdmin: Boolean) = {

    val ds = getDataSource(databaseConfig)

    implicit val connection = ds.getConnection

    val initDbStatements = separateSqlStatements(stripComments(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/sql/init_system_db.sql"), "utf-8").mkString))

    logger.info("Dropping all tables and sequences")

    dropAllTables(connection, logger)

    logger.info("Initialising database")

    initDbStatements.foreach {
      statement =>
        logger.debug(statement)
        SQL(statement).execute()
    }

    connection.close()

    if (createAdmin) {
      val sqlDataStore = new DataStoreSqlImpl(ds)

      logger.info("Creating the default admin user")

      // admin/intake24
      sqlDataStore.addUser("", SecureUserRecord("admin", "7klnEraBssvRBTnFR5FbIJ/5Qjqgf8w3/7Rs4gBoFBY=", "hUkIQLASWraVS4JDPOr8tA==", "shiro-sha256", Set("superuser", "admin"), Set(), Map()))
    }
  }
}
