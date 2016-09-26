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

package uk.ac.ncl.openlab.intake24.datastoresql.test

import anorm.SQL
import java.sql.DriverManager
import anorm.SqlParser

trait TestDB {

  val logger = org.slf4j.LoggerFactory.getLogger(classOf[TestDB])

  def separateSqlStatements(sql: String) =
    // Regex matches on semicolons that neither precede nor follow other semicolons
    sql.split("(?<!;);(?!;)").map(_.trim.replace(";;", ";")).filterNot(_.isEmpty)

  Class.forName("org.postgresql.Driver")

  implicit val dbConn = DriverManager.getConnection("jdbc:postgresql://localhost/intake24_system_test?user=postgres")

  val dropTableStatements =
    SQL("""SELECT 'DROP TABLE IF EXISTS ' || tablename || ' CASCADE;' AS query FROM pg_tables WHERE schemaname='public'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val dropSequenceStatements =
    SQL("""SELECT 'DROP SEQUENCE IF EXISTS ' || relname || ' CASCADE;' AS query FROM pg_class WHERE relkind = 'S'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val clearDbStatements = dropTableStatements ++ dropSequenceStatements

  def stripComments(s: String) = """(?m)/\*(\*(?!/)|[^*])*\*/""".r.replaceAllIn(s, "")
  
  val initDbStatements = separateSqlStatements(stripComments(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/sql/init_system_db.sql"), "utf-8").mkString))

  clearDbStatements.foreach {
    stmt =>
      logger.debug(stmt)
      SQL(stmt).execute()
  }

  initDbStatements.foreach {
    stmt =>
      logger.debug(stmt)
      SQL(stmt).execute()
  }
}
