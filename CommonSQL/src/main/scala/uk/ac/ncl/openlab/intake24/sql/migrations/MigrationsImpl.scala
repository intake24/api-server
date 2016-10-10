package uk.ac.ncl.openlab.intake24.sql.migrations

import javax.sql.DataSource
import anorm.SQL
import anorm.SqlParser
import java.util.UUID
import org.slf4j.LoggerFactory
import scala.Right
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import org.postgresql.util.PSQLException

sealed trait MigrationError

case class MigrationFailed(e: Throwable) extends MigrationError

case class SimpleDatabaseError(e: Throwable) extends MigrationError

class MigrationsImpl(val dataSource: DataSource) extends SqlDataService[SimpleDatabaseError] {

  private val logger = LoggerFactory.getLogger(classOf[MigrationsImpl])

  private def getCurrentVersion()(implicit conn: java.sql.Connection): Long =
    SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  def applyMigrations(migrations: Seq[Migration]): Either[MigrationError, Unit] = tryWithConnection {
    implicit conn =>

      def migrateFrom(version: Long): Either[MigrationError, Unit] = {
        val applicable = migrations.filter(_.versionFrom == version)
        if (applicable.isEmpty) {
          logger.info(s"No migrations left. Database schema is now at version $version.")
          Right(())
        } else if (applicable.size > 1) {
          Left(MigrationFailed(new RuntimeException("Found more than one migration from version $version. This is not allowed.")))
        } else {
          val migration = applicable(0)

          logger.info(s"Migrating to version ${migration.versionTo}: ${migration.description}")

          migration.apply(logger).right.map {
            _ =>
              SQL("UPDATE schema_version SET version={version}").on('version -> migration.versionTo).executeUpdate()
              logger.info(s"Migration to ${migration.versionTo} complete.")
          }.right.flatMap {
            _ => migrateFrom(migration.versionTo)
          }
        }
      }

      val version = getCurrentVersion()
      
      logger.info(s"Current database schema version: $version")

      migrateFrom(version) 
  }

  def defaultDatabaseError(e: PSQLException) = SimpleDatabaseError(e)
}