package uk.ac.ncl.openlab.intake24.systemsql.admin

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import uk.ac.ncl.openlab.intake24.api.shared.{NewRespondentIds, NewRespondentWithPhysicalData, NewUserProfile}
import uk.ac.ncl.openlab.intake24.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.UserPhysicalDataIn
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import uk.ac.ncl.openlab.intake24.systemsql.user.UserPhysicalDataServiceImpl

class UsersSupportServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource,
                                        usersService: UserAdminImpl,
                                        physicalDataService: UserPhysicalDataServiceImpl) extends UsersSupportService with SqlDataService {

  def createRespondentsWithPhysicalData(surveyId: String, newUsers: Seq[NewRespondentWithPhysicalData]): Either[DependentCreateError, Seq[NewRespondentIds]] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val newUsersInfo = newUsers.map {
          u => NewUserProfile(u.name, u.email, u.phone, Set(Roles.surveyRespondent(surveyId)), Map())
        }

        usersService.createUsersQuery(newUsersInfo).right.flatMap {
          newUserIds =>
            val usersWithIds = newUsers.zip(newUserIds)

            val newUserAliases = usersWithIds.map {
              case (newUserData, userId) => (userId, newUserData.externalId)
            }.toMap

            val physicalData = usersWithIds.map {
              case (newUserData, userId) =>
                (userId, UserPhysicalDataIn(newUserData.sex, newUserData.birthdate, newUserData.weight, newUserData.weightTarget, newUserData.height, None))
            }.toMap

            for (authTokens <- usersService.createSurveyUserAliasesQuery(surveyId, newUserAliases).right;
                 _ <- physicalDataService.batchUpdateQuery(physicalData).right
            ) yield usersWithIds.map {
              case (newUserData, userId) =>
                NewRespondentIds(userId, newUserData.externalId, authTokens(userId))
            }
        }
      }
  }
}