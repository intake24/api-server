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

lazy val apiShared = Project(id = "apiShared", base = file("ApiShared"))

lazy val sharedTypes = Project(id = "sharedTypes", base = file("SharedTypes"))

lazy val phraseSearch = Project(id = "phrasesearch", base = file ("PhraseSearch"))

lazy val gwtShared = Project(id = "gwtShared", base = file("ClientShared"))

lazy val dataStore = Project(id = "dataStore", base = file("DataStore")).dependsOn(gwtShared)

lazy val services = Project(id = "services", base = file("FoodLookupService")).dependsOn(sharedTypes, phraseSearch)

lazy val foodDataXml = Project(id = "foodDataXml", base = file("FoodDataXML")).dependsOn(services, sharedTypes)

lazy val foodDataSql = Project(id = "foodDataSql", base = file("FoodDataSQL")).dependsOn(services % "compile->compile;test->test", sharedTypes, foodDataXml)

lazy val dataStoreMongo = Project(id = "dataStoreMongo", base = file ("DataStoreMongo")).dependsOn(services, dataStore)

lazy val dataStoreSql = Project(id = "dataStoreSql", base = file("DataStoreSQL")).dependsOn(services % "compile->compile;test->test", dataStore, sharedTypes, dataStoreMongo)

lazy val apiPlayServer = Project(id = "apiPlayServer", base = file("ApiPlayServer")).enablePlugins(PlayScala, SystemdPlugin).dependsOn(foodDataSql, dataStoreSql, apiShared, services)

lazy val siteTest = Project(id = "siteTest", base = file("SiteTest"))

lazy val apiDocs = scalatex.ScalatexReadme(
  projectId = "ApiDocs",
  wd = file(""),
  url = "",
  source = "ApiDocs"
).settings(
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq (
    "com.lihaoyi" %% "upickle" % "0.3.7",
    "com.google.code.gson" % "gson" % "2.3.1" // for JSON pretty-printing
  )
).dependsOn(sharedTypes, apiShared, services)

