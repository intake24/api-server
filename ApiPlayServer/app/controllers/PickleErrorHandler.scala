package controllers

import play.api.mvc.Result
import play.api.libs.json.Json
import upickle.Invalid
import play.api.mvc.Action
import play.api.mvc.Results._
import scala.concurrent.Future

trait PickleErrorHandler {
  def tryWithPickle(block: => Result) =
    try {
      block
    } catch {
      case Invalid.Data(_, msg) => BadRequest(Json.obj("error" -> "json_exception", "message" -> msg))
      case Invalid.Json(msg, input) => BadRequest(Json.obj("error" -> "json_exception", "message" -> msg))
    }

  def tryWithPickleAsync(block: => Future[Result]) =
    try {
      block
    } catch {
      case Invalid.Data(_, msg) => Future.successful(BadRequest(Json.obj("error" -> "json_exception", "message" -> msg)))
      case Invalid.Json(msg, input) => Future.successful(BadRequest(Json.obj("error" -> "json_exception", "message" -> msg)))
    }
}