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

package net.scran24.survey

sealed trait PortionSize

case class AsServed(portionImage: String, portionWeight: Double, leftoversImage: Option[String], leftoversWeight: Double) extends PortionSize

case class Guide (portionChoice: Int, portionWeight: Double, leftoversChoice: Option[Int], leftoversWeight: Double, quantity: Double) extends PortionSize

case class DrinkScale (choice: Int, fillLevel: Double, leftoversLevel: Double, fillVolume: Double, leftoversVolume: Double) extends PortionSize

case class Ignore (reason: String) extends PortionSize

case class Text (servingDescription: String, leftoversDescription: Option[String]) extends PortionSize



case class Food (code: String, ndnsCode: Int, searchTerm: String, portionSize: PortionSize, dbId: String)

case class Meal (hours: Int, minutes: Int, name: String, foods: Seq[Food])

case class Survey (startTime: Long, endTime: Long, log: String, meals: Seq[Meal])


object Guide {
  def mkGuide (portionChoice: Int, portionWeight: Double, leftoversChoice: Option[Integer], leftoversWeight: Double, quantity: Double) = 
    Guide (portionChoice, portionWeight, leftoversChoice.map (_.intValue()), leftoversWeight, quantity)
}