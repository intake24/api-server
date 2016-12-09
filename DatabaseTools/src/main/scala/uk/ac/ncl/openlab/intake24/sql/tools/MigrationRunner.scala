package uk.ac.ncl.openlab.intake24.sql.tools

import anorm.{SqlParser, _}

trait MigrationRunner extends DatabaseConnection with WarningMessage {

  def runMigration(versionFrom: Long, versionTo: Long, options: DatabaseConfigurationOptions)(block: java.sql.Connection => Unit): Unit = {
    val dbConfig = chooseDatabaseConfiguration(options)

    displayWarningMessage(s"Are you sure you want to apply this migration to ${dbConfig.host}/${dbConfig.database}?")

    val dataSource = getDataSource(dbConfig)
    implicit val connection = dataSource.getConnection

    try {
      connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ)
      connection.setAutoCommit(false)

      val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

      if (version != versionFrom) {
        throw new RuntimeException(s"Wrong schema version: expected $versionFrom, got $version")
      } else {


        block(connection)

        println("Updating schema version...")

        SQL("UPDATE schema_version SET version={version_to} WHERE version={version_from}").on('version_from -> versionFrom, 'version_to -> versionTo).execute()

        connection.commit()

        println("Done!")
      }
    } catch {
      case e: Throwable =>
        connection.rollback()
        throw e
    } finally {
      connection.close()
    }
  }
}