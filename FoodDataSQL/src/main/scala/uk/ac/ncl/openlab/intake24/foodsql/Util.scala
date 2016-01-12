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

package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection
import scala.collection.mutable.Buffer
import anorm.BatchSql

object Util {
  @deprecated("Create a pull request for anorm instead...")
  def batchKeys(sql: BatchSql)(implicit conn: Connection): Seq[Long] = {
    val stmt = sql.getFilledStatement(conn, true)
    val result = stmt.executeBatch()
    val keys = stmt.getGeneratedKeys()

    try {
      if (result.exists(_ != 1))
        throw new RuntimeException("Failed batch update")

      val buf = Buffer[Long]()

      while (keys.next()) {
        buf += keys.getLong("id")
      }

      buf
    } catch {
      case e: Throwable => throw e
    } finally {
      keys.close()
      stmt.close()
    }
  }
}
