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

name := "common-sql"

organization := "uk.ac.ncl.openlab.intake24"

description := "Intake24 shared SQL code"

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.4.1211",
  "org.postgresql" % "postgresql" % "9.4.1211" % "test",
  "com.typesafe.play" %% "anorm" % "2.5.2",
  "org.slf4j" % "slf4j-api" % "1.7.21"
)
