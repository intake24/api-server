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

name := "image-processor-imagemagick"

organization := "uk.ac.ncl.openlab.intake24"

description := "Intake24 ImageMagick image processor implementation"

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.apache.xmlgraphics" % "batik-svg-dom" % "1.7",
  "org.apache.xmlgraphics" % "batik-util" % "1.7",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.2",
  "net.sf.opencsv" % "opencsv" % "2.1",
  "org.im4java" % "im4java" % "1.4.0"
)

