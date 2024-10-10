package zio2demo

import zio.{ZIO, ZLayer, ZIOAppDefault, Scope, ULayer, URIO}
import zio2demo.controll.CarController
import zio2demo.storage.{DatabaseLive}
import zio2demo.storage.driver.{ConnectionPoolLive}
import zio2demo.storage.repositories.CarRepositoryLive
import zio2demo.service.CarServiceLive
import zio2demo.controll.CarControllerLive

import scala.util.chaining._
import zio.ZIOAppArgs

object MyDemoApp extends ZIOAppDefault {

  def program: URIO[CarController, Unit] = {
    for {
      _ <- CarController.registerNewCar(1, "Ford", "Focus", 1)
      _ <- CarController.registerNewCar(1, "Toyota", "Corolla", 2)
      _ <- CarController.registerNewCar(3, "Ford", "Focus", 3)
    } yield ()
  }

  val myLayer: ULayer[CarController] = (CarRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> CarServiceLive.live >>> CarControllerLive.live
  def run: ZIO[ZIOAppArgs & Scope, Nothing, Any] = program.provideLayer(myLayer).exitCode
}

