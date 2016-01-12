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

name := "services"

organization := "uk.ac.ncl.openlab.intake24"

description := "Intake24 services"

version := "15.9-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)
