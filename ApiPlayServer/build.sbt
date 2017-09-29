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

name := """intake24-api-server"""

description := "Intake24 Play Framework API server"

maintainer := "Ivan Poliakov <ivan.poliakov@ncl.ac.uk>"

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases/"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  guice,
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.mohiva" %% "play-silhouette" % "5.0.0",
  "io.circe" %% "circe-core" % "0.9.0-M1",
  "io.circe" %% "circe-generic" % "0.9.0-M1",
  "io.circe" %% "circe-parser" % "0.9.0-M1",
  "com.typesafe.play" %% "play-mailer" % "6.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.1",
  "com.twilio.sdk" % "twilio" % "7.7.0",
  "com.amazonaws" % "aws-java-sdk" % "1.11.155",
  "org.apache.shiro" % "shiro-core" % "1.2.3", // for v1.0 authentication support
  "com.typesafe.play" %% "play-integration-test" % "2.6.5" % "test"
)

// These are the default Java settings that go into conf/application.ini and are read by the start up
// script. application.ini is overriden by the deployment script.

javaOptions in Universal ++= Seq(
  "-J-Xmx320m",
  "-J-Xms128m",

  // Process is managed by systemd, play's pid was causing issues
  s"-Dpidfile.path=/dev/null",

  s"-Dconfig.file=/usr/share/${packageName.value}/conf/application-default.conf",

  // Use separate logger configuration file for production environment
  s"-Dlogger.file=/usr/share/${packageName.value}/conf/logger-default.xml"
)

// Exclude local (development) config files from .deb

val excludeFromDeb = Seq("application.conf", "logback.xml")

linuxPackageMappings ~= {
  _.map {
    m =>
      m.copy(mappings = m.mappings.filterNot(f => excludeFromDeb.contains(f._1.getName())))
  }.filter(_.mappings.nonEmpty)
}

routesGenerator := InjectedRoutesGenerator

// EclipseKeys.withSource := true

// EclipseKeys.preTasks := Seq(compile in Compile)
