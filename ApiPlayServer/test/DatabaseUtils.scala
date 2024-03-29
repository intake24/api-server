import anorm.{SQL, SqlParser}
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import play.api.db.Database
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader

import java.sql.Connection

object StatementResource extends resource.Resource[java.sql.Statement] {
  def close(stmt: java.sql.Statement) = stmt.close()
}

trait DatabaseUtils extends SqlResourceLoader {

  private val logger = LoggerFactory.getLogger(classOf[DatabaseUtils])

  private def tryWithConnection[T](connection: Connection)(block: Connection => T): T = {
    try {
      block(connection)
    } catch {
      case e: Throwable => {
        if (!connection.getAutoCommit()) {
          connection.rollback()
        }
        throw e
      }
    } finally {
      connection.close()
    }
  }

  private def runQueriesFromResource(resourcePath: String)(implicit conn: Connection): Unit = {
    val statement = conn.createStatement()
    val sql = sqlFromResource(resourcePath)

    statement.execute(sql)
    statement.close()
  }

  def ensureEmpty(database: Database): Unit = tryWithConnection(database.getConnection()) {
    implicit conn =>
      val tableCount = SQL("select count(*) from information_schema.tables where table_schema = 'public'").executeQuery().as(SqlParser.scalar[Int].single)

      if (tableCount != 0) throw new RuntimeException("Test database is not empty. Will not proceed for data safety reasons -- please " +
        "make sure the test database is configured properly in application-test.conf")

      logger.debug("Database contains no tables")
  }

  def initSystem(database: Database): Unit = {
    val connection = database.getConnection()

    tryWithConnection(connection) {
      implicit conn =>
        runQueriesFromResource("intake24_system_v116_schema.sql")
        runQueriesFromResource("intake24_system_v116_data.sql")
    }

    // Queries generated by Postgres db_dump mess with connection settings so need to make sure this connection does not linger
    // in the pool.

    database.dataSource.asInstanceOf[HikariDataSource].evictConnection(connection)
  }

  def cleanup(database: Database): Unit = tryWithConnection(database.getConnection()) {
    implicit conn =>
      runQueriesFromResource("reset.sql")
  }
}
