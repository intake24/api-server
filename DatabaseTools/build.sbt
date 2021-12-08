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

name := "database-tools"

description := "Intake24 database tools"

assemblyMergeStrategy in assembly ~= { (old) => {
  case PathList("org", "w3c", "dom", xs@_*) => MergeStrategy.first
  case PathList("javax", "xml", xs@_*) => MergeStrategy.first
  case x => old(x)
}}

val circeVersion = "0.10.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.google.gwt" % "gwt-user" % "2.7.0" % "provided",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "com.zaxxer" % "HikariCP" % "2.5.1",
  "com.lihaoyi" %% "upickle" % "0.4.4",
  "org.rogach" %% "scallop" % "2.0.5",
  "commons-io" % "commons-io" % "2.5",
  "com.opencsv" % "opencsv" % "3.9",
  "org.apache.poi" % "poi-ooxml" % "4.1.0"
)

dependencyOverrides ++= Seq(
   "commons-beanutils" % "commons-beanutils" % "1.9.4",
)
