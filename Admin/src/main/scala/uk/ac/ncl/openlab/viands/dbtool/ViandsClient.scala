/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.viands.dbtool

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.localStorage
import net.scran24.fooddef.CategoryContents
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.Food
import uk.ac.ncl.openlab.intake24.api.Intake24Credentials
import upickle.default._
import net.scran24.fooddef.FoodGroup

sealed trait AuthenticationResult

object AuthenticationResult {
  case object Success extends AuthenticationResult
  case object InvalidCredentials extends AuthenticationResult   
}

case class InternalServerError(error: String, debugMessage: String) extends RuntimeException("Internal server error: " + error)

case class UnexpectedStatusCode(code: Int) extends RuntimeException("Unexpected status code: code.toString")

case class ViandsClient(apiBaseUrl: String) {  
  
  val JwtTokenKey = "viands_jwt"
  val DefaultTimeout = 5000
  val ContentTypeHeader = "Content-Type" -> "application/json"
  val AuthTokenHeader = "X-Auth-Token"
  
  private def url(action: String) = apiBaseUrl + action 
  
  def cachedToken = Option(localStorage.getItem(JwtTokenKey))
  
  def defaultHeaders = cachedToken match {
    case Some(token) => Map(ContentTypeHeader, AuthTokenHeader -> token)
    case None => Map(ContentTypeHeader)
  }
  
  def parseResponse[T](request: Future[XMLHttpRequest])(implicit ev: upickle.default.Reader[T]): Future[T]= request.map { 
    ajaxResult => ajaxResult.status match {
      case 200 => readJs[T](upickle.json.read(ajaxResult.responseText))
      case 500 => throw read[InternalServerError](ajaxResult.responseText)
      case code => throw UnexpectedStatusCode(code)
    }
  }
    
  private case class SuccessParser(token: String)
  
  def authenticate(credentials: Intake24Credentials): Future[AuthenticationResult] = {   
    Ajax.post(url("/signin"), write(credentials), DefaultTimeout, defaultHeaders).map {
      result => result.status match {
        case 200 => {
          val token = read[SuccessParser](result.responseText).token          
          localStorage.setItem(JwtTokenKey, token)
          AuthenticationResult.Success
        }
        case 401 => AuthenticationResult.InvalidCredentials
        case 500 => throw read[InternalServerError](result.responseText)
        case code => throw UnexpectedStatusCode(code) 
      }
    }
  }
  
  def rootCategories: Future[Seq[CategoryHeader]] =  
    parseResponse[Seq[CategoryHeader]](Ajax.get(url("/categories"), "", DefaultTimeout, defaultHeaders))
    
  def categoryContents(categoryCode: String): Future[CategoryContents] = 
    parseResponse[CategoryContents](Ajax.get(url("/categories/" + categoryCode), "", DefaultTimeout, defaultHeaders))
    
  def foodDef(code: String) =
    parseResponse[Food](Ajax.get(url(s"/foods/$code/definition"), "", DefaultTimeout, defaultHeaders))
    
  def allFoodGroups() =
    parseResponse[Seq[FoodGroup]](Ajax.get(url(s"/food-groups"), "", DefaultTimeout, defaultHeaders))
}