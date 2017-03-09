package controllers

import com.google.inject.Inject
import parsers.UpickleUtil
import play.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._
import uk.ac.ncl.openlab.intake24.errors._
import upickle.Js

import scala.collection.immutable.NumericRange
import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._

/**
  * Created by Tim Osadchiy on 09/02/2017.
  */
class DemographicGroupsController @Inject()(dgService: DemographicGroupsService,
                                            deadbolt: DeadboltActionsAdapter)
  extends Controller with ImageOrDatabaseServiceErrorHandler with UpickleUtil {

  def list() = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(dgService.list())
    }
  }

  def createDemographicGroup() = deadbolt.restrictToRoles(Roles.superuser)(upickleBodyParser[DemographicGroupRecordIn]) {
    request => Future {
      translateDatabaseResult(dgService.createDemographicGroup(request.body))
    }
  }

  def patchDemographicGroup(id: Int) = deadbolt.restrictToRoles(Roles.superuser)(upickleBodyParser[DemographicGroupRecordIn]) {
    request => Future {
      translateDatabaseResult(dgService.patchDemographicGroup(id, request.body))
    }
  }

  def getDemographicGroup(id: Int) = deadbolt.restrictToRoles(Roles.superuser) {
    request => Future {
      translateDatabaseResult(dgService.getDemographicGroup(id))
    }
  }

  def deleteDemographicGroup(id: Int) = deadbolt.restrictToRoles(Roles.superuser) {
    request => Future {
      translateDatabaseResult(dgService.deleteDemographicGroup(id))
    }
  }

  def createDemographicGroupScaleSector(demographicGroupId: Int) = deadbolt.restrictToRoles(Roles.superuser)(upickleBodyParser[DemographicScaleSectorIn]) {
    request => Future {
      translateDatabaseResult(dgService.createDemographicScaleSector(demographicGroupId, request.body))
    }
  }

  def patchDemographicGroupScaleSector(id: Int) = deadbolt.restrictToRoles(Roles.superuser)(upickleBodyParser[DemographicScaleSectorIn]) {
    request => Future {
      translateDatabaseResult(dgService.patchDemographicScaleSector(id, request.body))
    }
  }

  def deleteDemographicGroupScaleSector(id: Int) = deadbolt.restrictToRoles(Roles.superuser) {
    request => Future {
      translateDatabaseResult(dgService.deleteDemographicScaleSector(id))
    }
  }

}