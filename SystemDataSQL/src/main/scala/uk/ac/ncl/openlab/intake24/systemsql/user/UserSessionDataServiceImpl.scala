package uk.ac.ncl.openlab.intake24.systemsql.user

import java.time.ZonedDateTime

import anorm.{Macro, SQL}
import com.google.inject.Inject
import javax.inject.Named
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.errors.{CreateError, DeleteError, LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{SurveyService, UserSession, UserSessionDataService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

/**
  * Created by Tim Osadchiy on 21/03/2018.
  */
class UserSessionDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource, surveyService: SurveyService) extends UserSessionDataService with SqlDataService with SqlResourceLoader {

  private val TABLE_NAME = "user_sessions"
  private val FIELD_LIST = "user_id, survey_id, session_data, created"

  private val GET_Q = s"SELECT $FIELD_LIST FROM $TABLE_NAME WHERE survey_id={survey_id} AND user_id={user_id}"
  private val INSERT_Q =
    s"""
       |INSERT INTO user_sessions ($FIELD_LIST)
       |VALUES ({user_id}, {survey_id}, {session_data}, {created})
       |ON CONFLICT (user_id, survey_id)
       |  DO UPDATE SET session_data={session_data}, created={created}
       |  RETURNING $FIELD_LIST
    """.stripMargin
  private val DELETE_Q = s"DELETE FROM $TABLE_NAME WHERE survey_id = {survey_id} AND user_id = {user_id}"

  case class UserSessionRow(user_id: Long, survey_id: String, session_data: String, created: ZonedDateTime) {
    def toUserSession = UserSession(user_id, survey_id, session_data, created)
  }

  override def save(userSession: UserSession): Either[CreateError, UserSession] = tryWithConnection {
    implicit conn =>
      surveyService.getSurveyParameters(userSession.surveyId) match {
        case Right(surveyParameters) if surveyParameters.storeUserSessionOnServer =>
          val r = SQL(INSERT_Q).on('user_id -> userSession.userId, 'survey_id -> userSession.surveyId,
            'session_data -> userSession.sessionData, 'created -> userSession.created).executeQuery()
            .as(Macro.namedParser[UserSessionRow].single)
          Right(r.toUserSession)
        case Right(surveyParameters) if !surveyParameters.storeUserSessionOnServer =>
          Left(UnexpectedDatabaseError(new Exception(s"Survey ${userSession.surveyId} does not store user sessions")))
        case Left(error) => Left(UnexpectedDatabaseError(error.exception))
      }
  }

  override def get(surveyId: String, userId: Long): Either[LookupError, UserSession] = tryWithConnection {
    implicit conn =>
      val r = SQL(GET_Q).on('survey_id -> surveyId, 'user_id -> userId).executeQuery().as(Macro.namedParser[UserSessionRow].single)
      Right(r.toUserSession)
  }

  override def clean(surveyId: String, userId: Long): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      SQL(DELETE_Q).on('survey_id -> surveyId, 'user_id -> userId).execute()
      Right(());
  }

}
