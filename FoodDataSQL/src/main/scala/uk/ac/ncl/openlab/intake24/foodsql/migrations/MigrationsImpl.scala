package uk.ac.ncl.openlab.intake24.foodsql.migrations


import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import anorm.SQL
import anorm.SqlParser
import java.util.UUID
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService

class MigrationsImpl(val dataSource: DataSource) extends FoodDataSqlService {

  private val logger = LoggerFactory.getLogger(classOf[MigrationsImpl])

  private def getCompletedMigrationsQuery()(implicit conn: java.sql.Connection): Set[UUID] =
    SQL("SELECT id FROM schema_migrations").executeQuery().as(SqlParser.str("id").*).map(UUID.fromString(_)).toSet

  def applyNewMigrations(migrations: Seq[Migration]): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      val completed = getCompletedMigrationsQuery()
      val migrationsToRun = migrations.filterNot(m => completed.contains(m.id))

      migrationsToRun.foreach {
        migration =>
          logger.info(s"Applying migration ${migration.id}")
          migration.upgrade(logger)
      }

      Right(())
  }

}