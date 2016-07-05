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

package uk.ac.ncl.openlab.intake24.foodxml

import uk.ac.ncl.openlab.intake24.SplitList

object SplitListDef {
  def parseFile(path: String) = {
    val lines = scala.io.Source.fromFile(path).getLines().toList
    val splitWords = lines.head.split("\\s+").toList

    val z = Map[String, Set[String]]().withDefaultValue(Set())

    val pairs = lines.tail.foldLeft(z)((map, line) => {
      val words = line.split("\\s+").toList
      val key = words.head
      words.tail.foldLeft(map)((m, v) => m + (key -> (m(key) + v)))
    })

    SplitList(splitWords, pairs)
  }
}