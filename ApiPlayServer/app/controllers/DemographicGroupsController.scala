package controllers

import com.google.inject.Inject
import parsers.UpickleUtil
import play.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._
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

  implicit private val rangeWriter = upickle.default.Writer[NumericRange[Double]] {
    r =>
      Js.Obj(("start", Js.Num(r.start)), ("end", Js.Num(r.end)))

  }

  def list() = deadbolt.restrictAccess(Roles.superuser) {
    Future {
      translateDatabaseResult(dgService.list())
    }
  }

}
