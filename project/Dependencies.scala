import sbt._

object Dependencies {

  val zioVersion = "2.1.11"
  val zioHttpVersion = "3.0.1"

  val zioHttp     = "dev.zio" %% "zio-http"     % zioHttpVersion

  val zioTest     = "dev.zio" %% "zio-test"     % zioVersion % Test
  val zioTestSBT = "dev.zio" %% "zio-test-sbt" % zioVersion % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % zioVersion % Test  
}
