package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.api.shared.{NewRespondentIds, NewRespondentWithPhysicalData}
import uk.ac.ncl.openlab.intake24.errors._


// Binds users admin service and physical data service together
trait UsersSupportService {

  def createRespondentsWithPhysicalData(surveyId: String, newUsers: Seq[NewRespondentWithPhysicalData]): Either[DependentCreateError, Seq[NewRespondentIds]]
}