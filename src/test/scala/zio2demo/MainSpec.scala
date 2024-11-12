package zio2demo

import zio._
import zio.test._

object MainSpec extends ZIOSpecDefault {
  def spec: Spec[
    zio2demo.MainSpec.Environment & (zio.test.TestEnvironment & zio.Scope), Any
      ] = ???
}