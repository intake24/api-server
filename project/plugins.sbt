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

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.9")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")

addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "3.1.3")

// addDependencyTreePlugin

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

libraryDependencies ++= Seq(
  "org.vafer" % "jdeb" % "1.10" artifacts (Artifact("jdeb", "jar", "jar")),
)

dependencyOverrides ++= Seq(
  // There seems to be some conflict involving this library among the various SBT plugins which
  // breaks the dependency check plugin.
  // Forcing a dependency on a newer version helps but might cause some unexpected behaviour
  // if they aren't binary compatible.
  "com.google.guava" % "guava" % "27.1-jre"
)