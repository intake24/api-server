name := """intake24-data-export"""
organization := "uk.ac.ncl.openlab.intake24"

version := "4.3.1-SNAPSHOT"

scalaVersion := "2.12.14"

maintainer := "Ivan Poliakov <ivan.poliakov@ncl.ac.uk>"

libraryDependencies ++= Seq(
  jdbc,
  guice,
  cacheApi,
  ehcache,
  "commons-net" % "commons-net" % "3.6",
  "com.opencsv" % "opencsv" % "4.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.51",
  "com.typesafe.play" %% "play-mailer" % "6.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

dependencyOverrides ++= Seq(
  "net.sf.ehcache" % "ehcache" % "2.10.9.2",
  "com.atlassian.jwt" % "jwt-core" % "3.2.1",
  "com.atlassian.jwt" % "jwt-api" % "3.2.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5.1",
  "commons-beanutils" % "commons-beanutils" % "1.9.4",
  "com.typesafe.akka" % "akka-http-core_2.12" % "10.1.14",
  "com.typesafe.play" % "play-ws_2.12" % "2.6.24"
)
