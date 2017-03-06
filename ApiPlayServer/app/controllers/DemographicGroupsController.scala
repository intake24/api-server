package controllers

import com.google.inject.Inject
import parsers.UpickleUtil
import play.api.http.ContentTypes
import play.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import upickle.default.write

/**
  * Created by Tim Osadchiy on 09/02/2017.
  */
class DemographicGroupsController @Inject()(dgService: DemographicGroupsService,
                                            deadbolt: DeadboltActionsAdapter)
  extends Controller with ImageOrDatabaseServiceErrorHandler with UpickleUtil {

  def list() = deadbolt.restrictAccess(Roles.superuser) {
    Future {
      translateDatabaseResult(dgService.list())
    }
  }

  def createDemographicGroup() = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[DemographicGroupRecordIn]) {
    request => Future {
      translateDatabaseResult(dgService.createDemographicGroup(request.body))
    }
  }

  def patchDemographicGroup(id: Int) = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[DemographicGroupRecordIn]) {
    request => Future {
      translateDatabaseResult(dgService.patchDemographicGroup(id, request.body))
    }
  }

  def getDemographicGroup(id: Int) = deadbolt.restrictAccess(Roles.superuser) {
    request => Future {
      translateDatabaseResult(dgService.getDemographicGroup(id))
    }
  }

  def deleteDemographicGroup(id: Int) = deadbolt.restrictAccess(Roles.superuser) {
    request => Future {
      translateDatabaseResult(dgService.deleteDemographicGroup(id))
    }
  }

  def createDemographicGroupScaleSector(demographicGroupId: Int) = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[DemographicScaleSectorIn]) {
    request => Future {
      translateDatabaseResult(dgService.createDemographicScaleSector(demographicGroupId, request.body))
    }
  }

  def patchDemographicGroupScaleSector(id: Int) = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[DemographicScaleSectorIn]) {
    request => Future {
      translateDatabaseResult(dgService.patchDemographicScaleSector(id, request.body))
    }
  }

  def deleteDemographicGroupScaleSector(id: Int) = deadbolt.restrictAccess(Roles.superuser) {
    _ => Future {
      translateDatabaseResult(dgService.deleteDemographicScaleSector(id))
    }
  }

  def getHenryCoefficients() = deadbolt.restrictAccess(Roles.superuser, Roles.survey_admin, Roles.survey_respondent) {
    _ => Future {

      val result = Seq(
        HenryCoefficients("m", IntRange(0 ,3), 28.2, 859, -371),
        HenryCoefficients("m", IntRange(3 ,10), 15.1, 313, 306),
        HenryCoefficients("m", IntRange(10 ,18), 15.6, 266, 299),
        HenryCoefficients("m", IntRange(18 ,30), 14.4, 313, 113),
        HenryCoefficients("m", IntRange(30 ,60), 11.4, 541, -137),
        HenryCoefficients("m", IntRange(60, Int.MaxValue), 11.4, 541, -256),

        HenryCoefficients("f", IntRange(0, 3), 30.4, 703, -287),
        HenryCoefficients("f", IntRange(3, 10), 15.9, 210, 349),
        HenryCoefficients("f", IntRange(10, 18), 9.4, 249, 462),
        HenryCoefficients("f", IntRange(18, 30), 10.4, 615, -282),
        HenryCoefficients("f", IntRange(30, 60), 8.18, 502, -11.6),
        HenryCoefficients("f", IntRange(60, Int.MaxValue), 8.52, 421, 10.7)
      )

      Ok(write(result)).as(ContentTypes.JSON)
    }
  }

}

case class HenryCoefficients(sex: String, ageRange: IntRange, weightCoefficient: Double,
                             heightCoefficient: Double, constant: Double)