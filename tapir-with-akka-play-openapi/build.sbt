name := """play-with-tapir"""
organization := "com.example"

version := "1.0-SNAPSHOT"

/*
Possibly conflicting versions [2.6.4, 2.6.3] in libraries [akka-actor:2.6.4, akka-slf4j:2.6.3,
akka-actor-typed:2.6.3, akka-protobuf-v3:2.6.4, akka-stream:2.6.4]
*/
libraryDependencies += "com.typesafe.play"  %% "play" % "2.8.1"
libraryDependencies += "com.typesafe.play" %% "play-streams" % "2.8.1"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.11"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies += guice

val tapirVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core",
  "com.softwaremill.sttp.tapir" %% "tapir-play-server",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-model",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http"
).map(_ % tapirVersion)

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
