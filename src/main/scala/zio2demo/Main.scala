package zio2demo

import zio.{ZIO, ZLayer, ZIOAppDefault, Scope, ULayer}
import zio2demo.controll.CarController
import zio2demo.storage.{DB}
import zio2demo.storage.driver.{ConnectionPool}
import zio2demo.storage.repositories.CarRepository
import zio2demo.service.CarService
import zio2demo.controll.CarController

import scala.util.chaining._
import zio.ZIOAppArgs

object MyDemoApp extends ZIOAppDefault {

  def program(carController: CarController): ZIO[Any, Nothing, Unit] = {
    for {
      _ <- carController.registerNewCar("1234", "Ford", "Focus")
      _ <- carController.registerNewCar("5678", "Toyota", "Corolla")
      _ <- carController.registerNewCar("1234", "Ford", "Focus")
    } yield ()
  }

  val myLayer: ULayer[CarController] = (CarRepository.live ++ (ConnectionPool.live >>> DB.live)) >>> CarService.live >>> CarController.live
  def run: ZIO[ZIOAppArgs & Scope, Nothing, Any] = ZIO.service[CarController].flatMap(program).provideLayer(myLayer).exitCode

  // override def run: ZIO[ZIOAppArgs & Scope, Nothing, Any] = ZLayer
  //   .make[CarController](
  //     CarController.live,
  //     CarService.live,
  //     CarRepository.live,
  //     DB.live,
  //     ConnectionPool.live
  //   )
  //   .build
  //   .map(_.get[CarController])
  //   .flatMap(program)
}

