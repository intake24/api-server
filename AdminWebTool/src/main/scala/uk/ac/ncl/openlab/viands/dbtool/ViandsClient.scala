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

import org.scalajs.dom._
import scala.scalajs.js
import org.scalajs.jquery._
import org.scalajs.jquery.JQueryXHR
import upickle.default._
import org.scalajs.dom.ext.Ajax
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global
import uk.ac.ncl.openlab.intake24.api.Intake24Credentials
import scala.concurrent.Future
import upickle.Invalid
import net.scran24.fooddef.Category
import upickle.Js
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.CategoryContents
import net.scran24.fooddef.Food
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.Category2
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.Prompt

sealed trait AuthenticationResult 

object AuthenticationResult {
  case class Success(token: String) extends AuthenticationResult
  case class Failure(errorMessage: String) extends AuthenticationResult
}

object ViandsClient {  
  val JwtTokenKey = "viands_jwt"
  val DefaultTimeout = 5000
  val ContentTypeHeader = "Content-Type" -> "application/json"
  val AuthTokenHeader = "X-Auth-Token"
  
  private def url(action: String) = JsGlobals.viandsApiHost + action 
  
  def cachedToken = Option(localStorage.getItem(JwtTokenKey))
  
  def defaultHeaders = cachedToken match {
    case Some(token) => Map(ContentTypeHeader, AuthTokenHeader -> token)
    case None => Map(ContentTypeHeader)
  }
    
  def authenticate(credentials: Intake24Credentials): Future[AuthenticationResult] = {   
    Ajax.post(url("/signin"), write(credentials), DefaultTimeout, defaultHeaders).map {
      result => result.status match {
        case 200 => {
          val auth = read[AuthenticationResult.Success](result.responseText)
          localStorage.setItem(JwtTokenKey, auth.token)
          auth          
        }
        case _ => read[AuthenticationResult.Failure](result.responseText)
      }
    }
  }
  
  def rootCategories(): Future[Seq[CategoryHeader]] =
    Ajax.get(url("/categories"), "", DefaultTimeout, defaultHeaders).map {
    result => result.status match {
      case 200 => read[Seq[CategoryHeader]](result.responseText)
      case _ => throw new RuntimeException("mjo")
    }
  }
  def categoryContents(code: String): Future[CategoryContents] =
    Ajax.get(url("categories/" + code), "", DefaultTimeout, defaultHeaders).map {
    result => result.status match {
      case 200 => read[CategoryContents](result.responseText)
      case _ => throw new RuntimeException("mjo")
    }
  }

  def foodDef(code: String): Future[Food] = ???

  def foodData(code: String): Future[FoodData] = ???

  def foodParentCategories(code: String): Future[Seq[String]] = ???

  def foodAllCategories(code: String): Future[Seq[String]] = ???

  def categoryParentCategories(code: String): Future[Seq[String]] = ???

  def categoryAllCategories(code: String): Future[Seq[String]] = ???

  def categoryDef(code: String): Future[Category2] = ???

  def asServedDef(id: String): Future[AsServedSet] = ???

  def guideDef(id: String): Future[GuideImage] = ???

  def drinkwareDef(id: String): Future[DrinkwareSet] = ???

  def associatedFoodPrompts(foodCode: String): Future[Seq[Prompt]] = ???

  def brandNames(foodCode: String): Future[Seq[String]]  = ???
    
}