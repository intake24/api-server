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

name := "system-data-sql"

description := "Intake24 SQL based system database"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

parallelExecution in Test := false

// test in assembly := {}

libraryDependencies ++= Seq(
  "com.google.gwt" % "gwt-user" % "2.7.0" % "provided",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)
