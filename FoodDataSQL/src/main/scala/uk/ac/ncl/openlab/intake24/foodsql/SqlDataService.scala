package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection
import javax.sql.DataSource

trait SqlDataService {
  val dataSource: DataSource
  
   def tryWithConnection[T](block: Connection => T) = {
    val conn = dataSource.getConnection()
    try {
      block(conn)
    } catch {
      case e: java.sql.BatchUpdateException => throw new RuntimeException(e.getNextException)
      case e: Throwable => throw new RuntimeException(e)
    } finally {
      conn.close()
    }
  } 
}
