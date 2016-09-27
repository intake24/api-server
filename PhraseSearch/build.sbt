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

name := "phrase-search"

organization := "org.workcraft"

description := "Indexing and searching tools for short phrases"

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

javacOptions ++= Seq("-source", "1.6", "-target", "1.8", "-encoding", "UTF-8")

resolvers += Resolver.mavenLocal // for InfiAuto library

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "com.infiauto" % "infiauto-datastr" % "0.3.3",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)
