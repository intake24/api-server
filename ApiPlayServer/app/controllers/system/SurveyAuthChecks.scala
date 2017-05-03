package controllers.system

import security.Intake24AccessToken
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles


trait SurveyAuthChecks {

  def canListSurveys(t: Intake24AccessToken) = t.roles.exists(r => r.endsWith(Roles.staffSuffix) || r == Roles.superuser || r == Roles.surveyAdmin)

}
