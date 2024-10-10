package zio2

import zio._
import zio.test._

object MainSpec extends ZIOSpecDefault {
  def spec: Spec[
      demo.zio2.MainSpec.Environment & (zio.test.TestEnvironment & zio.Scope), Any
      ] = ???
}