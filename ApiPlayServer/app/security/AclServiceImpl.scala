package security

import javax.inject.Inject

import models.AccessSubject
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{UserAdminService, UserProfileUpdate}

/**
  * Created by Tim Osadchiy on 03/05/2017.
  */
class AclServiceImpl @Inject()(userAdminService: UserAdminService) extends AclService {

  def canPatchUser[T](subject: AccessSubject, userId: Long): Either[AnyError, Boolean] = {
    // Forbid changing your own password for now until e-mail/SMS validation is working,
    // but allow survey admins/staff to change it for you

    if (subject.userRoles.contains(Roles.superuser))
    // Superuser can change anyone's password
      Right(true)
    else
      userAdminService.getUserById(userId).right.map {
        userProfile =>
          val userIsRespondent = userProfile.roles.filter(_.endsWith(Roles.respondentSuffix)).map(_.dropRight(Roles.respondentSuffix.length))

          // Global survey admin can change any respondent's password
          if (subject.userRoles.contains(Roles.surveyAdmin))
            userIsRespondent.nonEmpty
          else {
            // Survey staff can change passwords for users who are respondents in their surveys only
            val subjectIsStaff = subject.userRoles.filter(_.endsWith(Roles.staffSuffix)).map(_.dropRight(Roles.staffSuffix.length))
            userIsRespondent.forall(surveyId => subjectIsStaff.contains(surveyId))
          }
      }
  }

}
