package controllers

import com.google.inject.Inject
import io.circe.generic.auto._
import parsers.{HtmlSanitisePolicy, JsonBodyParser, JsonUtils}
import play.api.http.ContentTypes
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Tim Osadchiy on 09/02/2017.
  */
class DemographicGroupsController @Inject()(dgService: DemographicGroupsService,
                                            palService: PhysicalActivityLevelService,
                                            rab: Intake24RestrictedActionBuilder,
                                            jsonBodyParser: JsonBodyParser,
                                            val controllerComponents: ControllerComponents,
                                            implicit val executionContext: ExecutionContext) extends BaseController
  with ImageOrDatabaseServiceErrorHandler with JsonUtils {

  private def sanitiseDemographicScaleSector(demographicScaleSectorIn: DemographicScaleSectorIn): DemographicScaleSectorIn = {
    DemographicScaleSectorIn(HtmlSanitisePolicy.sanitise(demographicScaleSectorIn.name),
      demographicScaleSectorIn.description.map(d => HtmlSanitisePolicy.sanitise(d)),
      demographicScaleSectorIn.sentiment, demographicScaleSectorIn.range)
  }

  def list() = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(dgService.list())
      }
  }

  def createDemographicGroup() = rab.restrictToRoles(Roles.superuser)(jsonBodyParser.parse[DemographicGroupRecordIn]) {
    request =>
      Future {
        translateDatabaseResult(dgService.createDemographicGroup(request.body))
      }
  }

  def patchDemographicGroup(id: Int) = rab.restrictToRoles(Roles.superuser)(jsonBodyParser.parse[DemographicGroupRecordIn]) {
    request =>
      Future {
        translateDatabaseResult(dgService.patchDemographicGroup(id, request.body))
      }
  }

  def getDemographicGroup(id: Int) = rab.restrictToAuthenticated {
    request =>
      Future {
        translateDatabaseResult(dgService.getDemographicGroup(id))
      }
  }

  def deleteDemographicGroup(id: Int) = rab.restrictToRoles(Roles.superuser) {
    _ =>
      Future {
        translateDatabaseResult(dgService.deleteDemographicGroup(id))
      }
  }

  def createDemographicGroupScaleSector(demographicGroupId: Int) = rab.restrictToRoles(Roles.superuser)(jsonBodyParser.parse[DemographicScaleSectorIn]) {
    request =>
      Future {
        val sanitised = sanitiseDemographicScaleSector(request.body)
        translateDatabaseResult(dgService.createDemographicScaleSector(demographicGroupId, sanitised))
      }
  }

  def patchDemographicGroupScaleSector(id: Int) = rab.restrictToRoles(Roles.superuser)(jsonBodyParser.parse[DemographicScaleSectorIn]) {
    request =>
      Future {
        val sanitised = sanitiseDemographicScaleSector(request.body)
        translateDatabaseResult(dgService.patchDemographicScaleSector(id, sanitised))
      }
  }

  def deleteDemographicGroupScaleSector(id: Int) = rab.restrictToRoles(Roles.superuser) {
    _ =>
      Future {
        translateDatabaseResult(dgService.deleteDemographicScaleSector(id))
      }
  }

  def getHenryCoefficients() = rab.restrictToAuthenticated {
    _ =>
      Future {

        val result = Seq(
          HenryCoefficients("m", IntRange(0, 3), 28.2, 859, -371),
          HenryCoefficients("m", IntRange(3, 10), 15.1, 313, 306),
          HenryCoefficients("m", IntRange(10, 18), 15.6, 266, 299),
          HenryCoefficients("m", IntRange(18, 30), 14.4, 313, 113),
          HenryCoefficients("m", IntRange(30, 60), 11.4, 541, -137),
          HenryCoefficients("m", IntRange(60, Int.MaxValue), 11.4, 541, -256),

          HenryCoefficients("f", IntRange(0, 3), 30.4, 703, -287),
          HenryCoefficients("f", IntRange(3, 10), 15.9, 210, 349),
          HenryCoefficients("f", IntRange(10, 18), 9.4, 249, 462),
          HenryCoefficients("f", IntRange(18, 30), 10.4, 615, -282),
          HenryCoefficients("f", IntRange(30, 60), 8.18, 502, -11.6),
          HenryCoefficients("f", IntRange(60, Int.MaxValue), 8.52, 421, 10.7)
        )

        Ok(toJsonString(result)).as(ContentTypes.JSON)
      }
  }

  def getPhysicalActivityLevels() = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(palService.list())
      }
  }

  def getWeightTargets() = rab.restrictToAuthenticated {
    _ =>
      Future {
        val result = Seq(
          WeightTarget("keep_weight", "Keep weight", 0),
          WeightTarget("lose_weight", "Lose weight", -500),
          WeightTarget("gain_weight", "Gain weight", 500)
        )
        Ok(toJsonString(result)).as(ContentTypes.JSON)
      }
  }

}

case class HenryCoefficients(sex: String, ageRange: IntRange, weightCoefficient: Double,
                             heightCoefficient: Double, constant: Double)

case class WeightTarget(id: String, description: String, coefficient: Double)