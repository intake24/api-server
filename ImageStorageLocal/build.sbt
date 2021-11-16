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

name := "image-storage-local"

description := "Intake24 local image storage implementation"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "commons-io" % "commons-io" % "2.5",
  "org.scalatest" %% "scalatest" % "3.2.10" % "test"
)
