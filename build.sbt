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

lazy val commonSettings = Seq(
  organization := "uk.ac.ncl.openlab.intake24",
  version := "2.4.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  publishArtifact in (Compile, packageDoc) := false
)

lazy val sharedTypes = Project(id = "sharedTypes", base = file("SharedTypes")).settings(commonSettings: _*)

lazy val phraseSearch = Project(id = "phrasesearch", base = file ("PhraseSearch")).settings(commonSettings: _*)

lazy val gwtShared = Project(id = "gwtShared", base = file("ClientShared")).settings(commonSettings: _*)


lazy val commonSql = Project(id = "commonSql", base = file("CommonSQL")).settings(commonSettings: _*)


lazy val systemDataServices = Project(id = "systemDataServices", base = file("SystemDataServices")).dependsOn(gwtShared, sharedTypes).settings(commonSettings: _*)

lazy val systemDataMongo = Project(id = "systemDataMongo", base = file ("SystemDataMongo")).dependsOn(systemDataServices % "compile->compile;test->test").settings(commonSettings: _*)

lazy val systemDataSql = Project(id = "systemDataSql", base = file("SystemDataSQL")).dependsOn(commonSql, systemDataServices % "compile->compile;test->test").settings(commonSettings: _*)


lazy val foodDataServices = Project(id = "foodDataServices", base = file("FoodDataServices")).dependsOn(sharedTypes, phraseSearch).settings(commonSettings: _*)

lazy val foodDataXml = Project(id = "foodDataXml", base = file("FoodDataXML")).dependsOn(foodDataServices).settings(commonSettings: _*)

lazy val nutrientsCsv = Project(id = "nutrientsCsv", base = file("NutrientsCSV")).dependsOn(sharedTypes, foodDataServices).settings(commonSettings: _*)

lazy val foodDataSql = Project(id = "foodDataSql", base = file("FoodDataSQL")).dependsOn(commonSql, foodDataServices % "compile->compile;test->test", sharedTypes, foodDataXml, nutrientsCsv).settings(commonSettings: _*)


lazy val databaseTools = Project(id = "databaseTools", base = file("DatabaseTools")).dependsOn(commonSql, imageProcessorIM, systemDataMongo, systemDataSql, foodDataXml, foodDataSql).settings(commonSettings: _*)


lazy val imageStorageLocal = Project(id = "imageStorageLocal", base = file("ImageStorageLocal")).dependsOn(foodDataServices).settings(commonSettings: _*)

lazy val imageProcessorIM = Project(id = "imageProcessorIM", base = file("ImageProcessorIM")).dependsOn(foodDataServices).settings(commonSettings: _*)


lazy val apiPlayServer = Project(id = "apiPlayServer", base = file("ApiPlayServer")).enablePlugins(PlayScala, SystemdPlugin).dependsOn(foodDataSql, systemDataSql, imageStorageLocal, imageProcessorIM).settings(commonSettings: _*)

lazy val siteTest = Project(id = "siteTest", base = file("SiteTest")).settings(commonSettings: _*)

/* lazy val apiDocs = scalatex.ScalatexReadme(
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
).dependsOn(sharedTypes, foodDataServices) */
