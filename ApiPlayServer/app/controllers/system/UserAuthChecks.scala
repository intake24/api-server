package controllers.system

import javax.inject.{Inject, Singleton}

import security.Intake24AccessToken
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService

@Singleton
class UserAuthChecks @Inject()(userAdminService: UserAdminService) {

  def canListUsers(t: Intake24AccessToken) = t.roles.exists(r => r.endsWith(Roles.staffSuffix) || r == Roles.superuser || r == Roles.surveyAdmin)

  def canCreateUser(t: Intake24AccessToken) = t.roles.exists(r => r == Roles.superuser || r == Roles.surveyAdmin)

  def canPatchUser(userId: Long)(subject: Intake24AccessToken): Either[AnyError, Boolean] = {
    // Forbid changing your own password for now until e-mail/SMS validation is working,
    // but allow survey admins/staff to change it for you

    if (subject.roles.contains(Roles.superuser))
    // Superuser can change anyone's password
      Right(true)
    else
      userAdminService.getUserById(userId).right.map {
        userProfile =>
          val userIsRespondent = userProfile.roles.filter(_.endsWith(Roles.respondentSuffix)).map(_.dropRight(Roles.respondentSuffix.length))

          // Global survey admin can change any respondent's password
          if (subject.roles.contains(Roles.surveyAdmin))
            userIsRespondent.nonEmpty
          else {
            // Survey staff can change passwords for users who are respondents in their surveys
            val subjectIsStaff = subject.roles.filter(_.endsWith(Roles.staffSuffix)).map(_.dropRight(Roles.staffSuffix.length))
            subjectIsStaff.exists(surveyId => userIsRespondent.contains(surveyId))
          }
      }
  }

  def canDeleteUser(userId: Long)(subject: Intake24AccessToken) = canPatchUser(userId)(subject) // Is this correct?
}
