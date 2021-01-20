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

name := """intake24-play-security"""

description := "Intake24 security module for Play framework "

maintainer := "Ivan Poliakov <ivan.poliakov@ncl.ac.uk>"

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases/"

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.mohiva" %% "play-silhouette" % "5.0.5",
  "io.circe" %% "circe-core" % "0.9.3",
  "io.circe" %% "circe-generic" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "org.apache.shiro" % "shiro-core" % "1.2.3", // for v1.0 authentication support
)

