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

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import uk.ac.ncl.openlab.intake24.PortionSizeMethod

object Util {
  def conditional[A](cond: Boolean, value: A) = if (cond) Some(value) else None

  def isUndefined(s: String) = s == "(undefined)"

  def checkPortionSize(m: PortionSizeMethod): Seq[String] = {
    val param = m.parameters.map(p => (p.name, p.value)).toMap

    m.method match {
      case "as-served" =>
        Seq(conditional(isUndefined(param("serving-image-set")), "Serving image set must be defined")).flatten
      case "drink-scale" =>
        Seq(conditional(isUndefined(param("drinkware-id")), "Drinkware set id must be defined")).flatten
      case "guide-image" =>
        Seq(conditional(isUndefined(param("guide-image-id")), "Guide image id must be defined")).flatten
      case "standard-portion" =>
        Seq(conditional(param.get("units-count").map(_.toInt).getOrElse(0) == 0, "At least one standard unit must be defined")).flatten
      case "cereal" =>
        Seq()
      case "pizza" =>
        Seq()
      case "milk-on-cereal" =>
        Seq()
      case "milk-in-a-hot-drink" =>
        Seq()
      case x => Seq("Unrecognised portion size method name: " + x)
    }
  }
}