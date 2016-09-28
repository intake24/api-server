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

lazy val sharedTypes = Project(id = "sharedTypes", base = file("SharedTypes"))

lazy val phraseSearch = Project(id = "phrasesearch", base = file ("PhraseSearch"))

lazy val gwtShared = Project(id = "gwtShared", base = file("ClientShared"))


lazy val commonSql = Project(id = "commonSql", base = file("CommonSQL"))


lazy val systemDataServices = Project(id = "systemDataServices", base = file("SystemDataServices")).dependsOn(gwtShared)

lazy val systemDataMongo = Project(id = "systemDataMongo", base = file ("SystemDataMongo")).dependsOn(systemDataServices % "compile->compile;test->test")

lazy val systemDataSql = Project(id = "systemDataSql", base = file("SystemDataSQL")).dependsOn(commonSql, systemDataServices % "compile->compile;test->test")


lazy val foodDataServices = Project(id = "foodDataServices", base = file("FoodDataServices")).dependsOn(sharedTypes, phraseSearch)

lazy val foodDataXml = Project(id = "foodDataXml", base = file("FoodDataXML")).dependsOn(foodDataServices)

lazy val nutrientsCsv = Project(id = "nutrientsCsv", base = file("NutrientsCSV")).dependsOn(sharedTypes, foodDataServices)

lazy val foodDataSql = Project(id = "foodDataSql", base = file("FoodDataSQL")).dependsOn(commonSql, foodDataServices % "compile->compile;test->test", sharedTypes, foodDataXml, nutrientsCsv)


lazy val databaseTools = Project(id = "databaseTools", base = file("DatabaseTools")).dependsOn(systemDataMongo, systemDataSql, foodDataXml, foodDataSql)

lazy val apiPlayServer = Project(id = "apiPlayServer", base = file("ApiPlayServer")).enablePlugins(PlayScala, SystemdPlugin).dependsOn(foodDataSql, systemDataSql)

lazy val siteTest = Project(id = "siteTest", base = file("SiteTest"))

lazy val apiDocs = scalatex.ScalatexReadme(
  projectId = "ApiDocs",
  wd = file(""),
  url = "",
  source = "ApiDocs"
).settings(
  scalaVersion := "2.11.8",
  libraryDependencies ++= Seq (
    "com.lihaoyi" %% "upickle" % "0.4.1",
    "com.google.code.gson" % "gson" % "2.3.1" // for JSON pretty-printing
  )
).dependsOn(sharedTypes, foodDataServices)
