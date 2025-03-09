import Dependencies._

ThisBuild / organization := "chrto"
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion      := "3.3.3"
ThisBuild / fork              := true
ThisBuild / scalacOptions     := optionsOnOrElse("2.13", "2.12")("-Ywarn-unused")("").value
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies ++= List("com.github.liancheng" %% "organize-imports" % "0.6.0")

def settingsApp = Seq(
  name := "zio2-demp",
  Compile / run / mainClass := Option("zio2demo.MyHttpApp"),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  libraryDependencies ++= Seq(
    zioHttp,
    zioUuid,
    zioJson,
    jwtCore,
    zioTest,
    zioTestSBT,
    zioTestMagnolia,
    zioMock,
    cats
  ),
)

def settingsDocker = Seq(
  Docker / version          := version.value,
  dockerBaseImage := "eclipse-temurin:20.0.1_9-jre",
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(settingsApp)
  .settings(settingsDocker)

addCommandAlias("fmt", "scalafmt; Test / scalafmt; sFix;")
addCommandAlias("fmtCheck", "scalafmtCheck; Test / scalafmtCheck; sFixCheck")
addCommandAlias("sFix", "scalafix OrganizeImports; Test / scalafix OrganizeImports")
addCommandAlias("sFixCheck", "scalafix --check OrganizeImports; Test / scalafix --check OrganizeImports")
