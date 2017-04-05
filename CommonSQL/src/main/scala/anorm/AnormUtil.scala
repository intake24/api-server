package anorm

import scala.collection.mutable.Buffer
import java.sql.Connection
import org.slf4j.LoggerFactory

object AnormUtil {
  
  private val logger = LoggerFactory.getLogger("uk.ac.ncl.openlab.intake24.AnormUtil")

  def escapeLike(s: String) = s.replace("\\", "\\\\").replace("_", "\\_").replace("%", "\\%")

  def isNull(row: Row, columnName: String): Boolean = row.get(columnName).get._1 == null

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
