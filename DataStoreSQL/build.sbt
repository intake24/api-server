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

name := "datastore-sql"

organization := "uk.ac.ncl.openlab.intake24"

description := "Intake24 SQL based survey database"

version := "15.9-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.5",
  "com.google.gwt" % "gwt-user" % "2.7.0",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41" % "test",
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "com.typesafe.play" %% "anorm" % "2.5.0",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "slf4j-simple" % "1.7.12", 
  "org.scalatest" %% "scalatest" % "2.2.5" % "test",
  "org.rogach" %% "scallop" % "0.9.5"
)
