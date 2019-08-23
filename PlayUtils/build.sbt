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

name := """intake24-play-utils"""

description := "Intake24 helpers for Play framework "

maintainer := "Ivan Poliakov <ivan.poliakov@ncl.ac.uk>"

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.6.16",
  "io.circe" %% "circe-core" % "0.10.1",
  "io.circe" %% "circe-generic" % "0.10.1",
  "io.circe" %% "circe-parser" % "0.10.1"
)

