package uk.ac.ncl.openlab.intake24.systemsql.admin

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SigninAttempt, SigninLogService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import anorm.SQL

class SigninLogImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SigninLogService with SqlDataService with SqlResourceLoader {

  def logSigninAttempt(event: SigninAttempt): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("INSERT INTO signin_log VALUES (DEFAULT, DEFAULT,{ip},{provider},{provider_key},{success},{userId},{message},{user_agent})")
        .on('ip -> event.remoteAddress, 'provider -> event.provider, 'provider_key -> event.providerKey, 'success -> event.success, 'userId -> event.userId, 'message -> event.message,
        'user_agent -> event.userAgent)
        .execute()

      Right(())
  }
}
