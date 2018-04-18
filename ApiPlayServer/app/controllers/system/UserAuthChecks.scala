package controllers.system

import javax.inject.{Inject, Singleton}
import javax.security.auth.Subject

import security.Intake24AccessToken
import uk.ac.ncl.openlab.intake24.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{UserAdminService, UserProfile}

@Singleton
class UserAuthChecks @Inject()(userAdminService: UserAdminService) {

  def canListUsers(t: Intake24AccessToken) = t.roles.exists(r => r.endsWith(Roles.staffSuffix) || r == Roles.superuser || r == Roles.surveyAdmin)

  def canCreateUser(t: Intake24AccessToken) = t.roles.exists(r => r == Roles.superuser || r == Roles.surveyAdmin)

  def getSurveyIdsWhereUserIsRespondent(profile: UserProfile): Set[String] =
    profile.roles.filter(_.endsWith(Roles.respondentSuffix)).map(_.dropRight(Roles.respondentSuffix.length))

  def getSurveyIdsWhereSubjectIsStaff(subject: Intake24AccessToken): Set[String] =
    subject.roles.filter(_.endsWith(Roles.staffSuffix)).map(_.dropRight(Roles.staffSuffix.length))

  def isUserRespondentOnly(profile: UserProfile): Boolean =
    profile.roles.nonEmpty && profile.roles.forall(_.endsWith(Roles.respondentSuffix))

  def canUpdateProfile(userId: Long)(subject: Intake24AccessToken): Either[DatabaseError, Boolean] = {
    // Anyone can edit their own profile
    if (subject.userId == userId)
      Right(true)
    else if (subject.roles.contains(Roles.superuser))
    // Superuser can edit anyone's profile
      Right(true)
    else
      userAdminService.getUserById(userId).right.map {
        userProfile =>
          // Global survey admin can edit any respondent's profile if they only have respondent roles
          if (subject.roles.contains(Roles.surveyAdmin))
            isUserRespondentOnly(userProfile)
          else {
            // Survey staff can edit profiles for users who are respondents of their surveys only
            // e.g. if user is respondent for s1 and s2, and subject is staff for s1, s2, s3, request will be allowed
            // however if user is also respondent for s4 (where the subject isn't staff), request will be denied

            val userIsRespondent = getSurveyIdsWhereUserIsRespondent(userProfile)
            val subjectIsStaff = getSurveyIdsWhereSubjectIsStaff(subject)

            userIsRespondent.forall(surveyId => subjectIsStaff.contains(surveyId))
          }
      }
  }

  def canUpdatePassword(userId: Long)(subject: Intake24AccessToken): Either[DatabaseError, Boolean] = {
    // Forbid changing your own password for now until e-mail/SMS validation is working
    if (subject.userId == userId)
      Right(false)
    else
    // Otherwise use the same logic as profile update
      canUpdateProfile(userId)(subject)
  }

  def canDeleteUser(userId: Long)(subject: Intake24AccessToken) = canUpdatePassword(userId)(subject) // Is this correct?
}
