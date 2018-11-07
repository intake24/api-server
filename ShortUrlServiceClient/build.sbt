name := """short-urls-client"""
organization := "uk.ac.ncl.openlab.intake24"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.7"

scalacOptions += "-Ypartial-unification"

val circeVersion = "0.10.1"
val http4sVersion = "0.20.0-M2"

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "4.2.2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.http4s" %% "http4s-core" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "com.typesafe" % "config" % "1.3.3"
)
