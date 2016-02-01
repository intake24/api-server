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

lazy val apiShared = crossProject.crossType(CrossType.Pure).in(file("ApiShared"))
  .settings(
    organization := "uk.ac.ncl.openlab.intake24",
    description := "Intake24 API shared types",
    version := "15.9-SNAPSHOT",
    scalaVersion := "2.11.7"
  )
  .jvmSettings (
    name := "api-shared-jvm"
  )
  .jsSettings (
    name := "api-shared-js"
  )

lazy val sharedTypes = crossProject.crossType(CrossType.Pure).in(file("SharedTypes"))
  .settings(
    organization := "uk.ac.ncl.openlab.intake24",
    description := "Intake24 basic shared types",
    version := "15.9-SNAPSHOT",
    scalaVersion := "2.11.7"
  )
  .jvmSettings (
    name := "shared-types-jvm"
  )
  .jsSettings (
    name := "shared-types-js"
  )

lazy val admin = Project(id = "admin", base = file("Admin"))
.settings (
  name := "admin",
  description := "Intake24 API-based admin tool",
  version := "15.9-SNAPSHOT",
  scalaVersion := "2.11.7",
  classDirectory in Compile := crossTarget.value,
  EclipseKeys.withSource := true,
  libraryDependencies ++= Seq(
	"be.doeraene" %%% "scalajs-jquery" % "0.8.1",
	"org.scala-js" %%% "scalajs-dom" % "0.8.2", 
	"com.lihaoyi" %%% "scalatags" % "0.5.4",
	"com.lihaoyi" %%% "upickle" % "0.3.7")
).enablePlugins(ScalaJSPlugin).dependsOn(apiSharedJs)

lazy val sharedTypesJvm = sharedTypes.jvm

lazy val sharedTypesJs = sharedTypes.js

lazy val apiSharedJvm = apiShared.jvm.dependsOn(sharedTypesJvm)

lazy val apiSharedJs = apiShared.js.dependsOn(sharedTypesJs)

lazy val phraseSearch = Project(id = "phrasesearch", base = file ("PhraseSearch"))

lazy val gwtShared = Project(id = "gwtShared", base = file("ClientShared"))

lazy val dataStore = Project(id = "dataStore", base = file("DataStore")).dependsOn(gwtShared)

lazy val services = Project(id = "services", base = file("FoodLookupService")).dependsOn(sharedTypesJvm, phraseSearch)

lazy val foodDataXml = Project(id = "foodDataXml", base = file("FoodDataXML")).dependsOn(services, sharedTypesJvm)

lazy val foodDataSql = Project(id = "foodDataSql", base = file("FoodDataSQL")).dependsOn(services % "compile->compile;test->test", sharedTypesJvm, foodDataXml)

lazy val dataStoreMongo = Project(id = "dataStoreMongo", base = file ("DataStoreMongo")).dependsOn(services, dataStore)

lazy val dataStoreSql = Project(id = "dataStoreSql", base = file("DataStoreSQL")).dependsOn(services % "compile->compile;test->test", dataStore, sharedTypesJvm, dataStoreMongo)

lazy val apiPlayServer = Project(id = "apiPlayServer", base = file("ApiPlayServer")).enablePlugins(PlayScala).dependsOn(foodDataSql, dataStoreSql, apiSharedJvm, services)

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
).dependsOn(sharedTypesJvm, apiSharedJvm, services)

