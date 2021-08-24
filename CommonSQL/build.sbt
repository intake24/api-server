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

description := "Intake24 shared SQL code"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.23",
  "org.postgresql" % "postgresql" % "42.2.23" % "test",
  "com.typesafe.play" %% "anorm" % "2.5.3",
  "org.slf4j" % "slf4j-api" % "1.7.21"
)
