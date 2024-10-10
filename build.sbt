name := "zio2-db-demo"

version := "0.1.0-SNAPSHOT"

scalaVersion := "3.3.3"

libraryDependencies ++= Seq(
  // zio
  "dev.zio" %% "zio" % "2.0.2",

  "dev.zio" %% "zio-test" % "2.0.2" % Test,
  "dev.zio" %% "zio-test-sbt" % "2.0.2" % Test,
)

ThisBuild / organization := "demo.zio2"
ThisBuild / version := "0.1.0-SNAPSHOT"