package zio2demo

import zio._
import zio.test._

object MainSpec extends ZIOSpecDefault {
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = test("...")(assertTrue(true))
}