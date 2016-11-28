package uk.ac.ncl.openlab.intake24.sql.tools

import anorm.{SqlParser, _}

trait MigrationRunner extends DatabaseConnection {

  def runMigration(versionFrom: Long, versionTo: Long, options: DatabaseConfigurationOptions)(block: java.sql.Connection => Unit): Unit = {
    val dbConfig = chooseDatabaseConfiguration(options)
    val dataSource = getDataSource(dbConfig)
    implicit val connection = dataSource.getConnection

    try {
      val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

      if (version != versionFrom) {
        throw new RuntimeException(s"Wrong schema version: expected $versionFrom, got $version")
      } else {
        block(connection)

        println("Updating schema version...")

        SQL("UPDATE schema_version SET version={version_to} WHERE version={version_from}").on('version_from -> versionFrom, 'version_to -> versionTo).execute()

        println("Done!")
      }
    } finally {
      connection.close()
    }
  }
}