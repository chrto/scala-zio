import sbt._

object Dependencies {

  val zioVersion = "2.1.11"
  val zioHttpVersion = "3.0.1"
  val zioUuidVersion = "1.0.0"
  val catsVersion = "2.12.0"

  val zioHttp     = "dev.zio" %% "zio-http"     % zioHttpVersion
  val zioUuid     = "com.guizmaii" %% "zio-uuid" % zioUuidVersion
  val zioTest     = "dev.zio" %% "zio-test"     % zioVersion % Test
  val zioTestSBT = "dev.zio" %% "zio-test-sbt" % zioVersion % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % zioVersion % Test

  val cats = "org.typelevel" %% "cats-core" % catsVersion
}
