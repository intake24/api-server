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

package uk.ac.ncl.openlab.viands.dbtool.ui.copy

import net.scran24.fooddef.Food
import org.scalajs.jquery.JQuery
import scalatags.JsDom.all._
import org.scalajs.dom.raw.Element
import net.scran24.fooddef.FoodGroup

case class FoodDef(originalFoodDef: Food, foodGroups: Seq[FoodGroup]) {

  val foodGroupOptions = foodGroups.map( g => option(g.englishDescription))
  
  val elem = div(
    div(
      label(`for` := "foodCode")("Intake24 code: "),
      input(`type` := "text", id := "code", value := originalFoodDef.code)),
    div(
      label("Description:"),
      input(`type` := "text", id := "description", value := originalFoodDef.englishDescription)),
    div(
        label("Food group:"),
        select(
            foodGroupOptions
            )
        
        )).render

}