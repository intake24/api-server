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
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreSqlImpl
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SecureUserRecord
import uk.ac.ncl.openlab.intake24.sql.SqlFileUtil
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfiguration, DatabaseConnection, DatabaseOptions, WarningMessage}
import uk.ac.ncl.openlab.intake24.systemsql.admin.UserAdminImpl

trait InitOptions extends ScallopConf {
  version("Intake24 SQL system database init tool 2.0.0-SNAPSHOT")

  val createAdmin = opt[Boolean](noshort = true)
  val noWarning = opt[Boolean](noshort = true)
}

object InitConsole extends App with WarningMessage {

  val options = new ScallopConf(args) with InitOptions with DatabaseOptions

  options.verify()

  if (!options.noWarning())
    displayWarningMessage("WARNING: THIS WILL DESTROY ALL DATA IN THE SYSTEM DATABASE!")

  Init.run(options.databaseConfig, options.createAdmin())
}

object Init extends DatabaseConnection with SqlFileUtil {

  val logger = LoggerFactory.getLogger(getClass)

  def run(databaseConfig: DatabaseConfiguration, createAdmin: Boolean) = {

    val ds = getDataSource(databaseConfig)

    implicit val connection = ds.getConnection

    val initDbStatements = separateSqlStatements(stripComments(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/sql/init_system_db_v11.sql"), "utf-8").mkString))

    val seedDbStatements = separateSqlStatements(stripComments(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/sql/init_system_db_v11_seed.sql"), "utf-8").mkString))

    logger.info("Dropping all tables and sequences")

    dropAllTables(connection, logger)

    logger.info("Initialising database")

    initDbStatements.foreach {
      statement =>
        logger.debug(statement)
        SQL(statement).execute()
    }

    seedDbStatements.foreach {
      statement =>
        logger.debug(statement)
        SQL(statement).execute()
    }

    connection.close()

    if (createAdmin) {
      val userAdminService = new UserAdminImpl(ds)

      logger.info("Creating the default admin user")

      userAdminService.createUser(None, SecureUserRecord("admin", "7klnEraBssvRBTnFR5FbIJ/5Qjqgf8w3/7Rs4gBoFBY=", "hUkIQLASWraVS4JDPOr8tA==", "shiro-sha256", Some("Intake24 Super User"), Some("support@intake24.co.uk"), None, Set("superuser", "admin"), Set(), Map())) match {
        case Left(e) => e.exception.printStackTrace()
        case _ => Right(())
      }
    }
  }
}
