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

name := """api-play-server"""

description := "Intake24 Play Framework API server"

maintainer := "Ivan Poliakov <ivan.poliakov@ncl.ac.uk>"

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases/"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.mohiva" %% "play-silhouette" % "4.0.0",
  "be.objectify" %% "deadbolt-scala" % "2.5.0",
  "com.lihaoyi" %% "upickle" % "0.4.1",
  "org.scalaz" %% "scalaz-core" % "7.2.6",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "org.apache.shiro" % "shiro-core" % "1.2.3", // for v1.0 authentication support
  "com.typesafe.play" % "play-integration-test_2.11" % "2.5.9" % "test"
)

javaOptions in Universal ++= Seq(
  // JVM memory tuning
  "-J-Xmx320m",
  "-J-Xms128m",

  // Since play uses separate pidfile we have to provide it with a proper path
  s"-Dpidfile.path=/dev/null",

  // Use separate configuration file for production environment
  s"-Dconfig.file=/usr/share/${packageName.value}/conf/production.conf",

  // Use separate logger configuration file for production environment
  s"-Dlogger.file=/usr/share/${packageName.value}/conf/production-logger.xml"
)

routesGenerator := InjectedRoutesGenerator

EclipseKeys.withSource := true

EclipseKeys.preTasks := Seq(compile in Compile)
