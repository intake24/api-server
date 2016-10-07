package uk.ac.ncl.openlab.intake24.sql.migrations


import javax.sql.DataSource
import anorm.SQL
import anorm.SqlParser
import java.util.UUID
import org.slf4j.LoggerFactory
import scala.Right
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import org.postgresql.util.PSQLException

case class SimpleDatabaseError(e: Throwable)

class MigrationsImpl(val dataSource: DataSource) extends SqlDataService[SimpleDatabaseError] {

  private val logger = LoggerFactory.getLogger(classOf[MigrationsImpl])

  private def getCompletedMigrationsQuery()(implicit conn: java.sql.Connection): Set[Long] =
    SQL("SELECT id FROM schema_migrations").executeQuery().as(SqlParser.long("id").*).toSet

  def applyNewMigrations(migrations: Seq[Migration]): Either[SimpleDatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      val completed = getCompletedMigrationsQuery()
      val migrationsToRun = migrations.filterNot(m => completed.contains(m.timestamp))

      migrationsToRun.sortBy(_.timestamp).foreach {
        migration =>
          logger.info(s"Applying migration: ${migration.description}")
          migration.apply(logger)
      }

      Right(())
  }

  def defaultDatabaseError(e: PSQLException) = SimpleDatabaseError(e)
}