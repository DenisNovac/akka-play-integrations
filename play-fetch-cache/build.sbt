name := """play-fetch-cache"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies += "ch.qos.logback"             % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"
libraryDependencies += "com.github.pureconfig"      %% "pureconfig"     % "0.12.3"

libraryDependencies += "org.typelevel" %% "cats-core"   % "2.1.1"
libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.2"
libraryDependencies += "com.47deg"     %% "fetch"       % "1.2.2"

libraryDependencies ++= Seq(
  caffeine
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
