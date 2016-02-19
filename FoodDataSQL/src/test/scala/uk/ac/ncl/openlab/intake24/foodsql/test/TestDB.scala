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

package uk.ac.ncl.openlab.intake24.foodsql.test

import anorm.SQL
import java.sql.DriverManager
import anorm.SqlParser
import uk.ac.ncl.openlab.intake24.foodsql.SqlFileUtil

trait TestDB extends SqlFileUtil {

  val logger = org.slf4j.LoggerFactory.getLogger(classOf[TestDB])

 
  Class.forName("org.postgresql.Driver")

  implicit val dbConn = DriverManager.getConnection("jdbc:postgresql://localhost/intake24_foods_test?user=postgres")

  val dropTableStatements =
    SQL("""SELECT 'DROP TABLE IF EXISTS ' || tablename || ' CASCADE;' AS query FROM pg_tables WHERE schemaname='public'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val dropSequenceStatements =
    SQL("""SELECT 'DROP SEQUENCE IF EXISTS ' || relname || ' CASCADE;' AS query FROM pg_class WHERE relkind = 'S'""")
      .executeQuery()
      .as(SqlParser.str("query").*)

  val clearDbStatements = dropTableStatements ++ dropSequenceStatements

  val initDbStatements = loadStatementsFromResource("/sql/init_foods_db.sql")
  
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