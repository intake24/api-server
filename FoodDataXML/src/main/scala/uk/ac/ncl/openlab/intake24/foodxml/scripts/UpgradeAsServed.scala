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

package uk.ac.ncl.openlab.intake24.foodxml.scripts

import scala.xml.XML

object UpgradeAsServed {
  def main(args: Array[String]) = {
    val asServedPath = "D:\\SCRAN24\\Data\\as-served-weight.xml"
    val asServedDestPath = "D:\\SCRAN24\\Data\\as-served-v2.xml" 
    //AsServedDef.writeXml(asServedDestPath, AsServedDef.parseXml(XML.load(asServedPath)).values.toSeq.sortBy(_.id))
  }
}