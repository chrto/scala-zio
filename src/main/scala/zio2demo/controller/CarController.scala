package zio2demo.controller

import zio.{ZIO, URLayer, ZLayer, UIO, URIO}

import scala.util.chaining._

import zio2demo.service.CarService
import zio2demo.model.Car
import zio2demo.model.error.{ServiceError, EntityExistsError}
import zio2demo.model.error.{DatabaseError, ConnectionNotAvailable}

// Define service
trait CarController {
  def register(id: Int, brand: String, model: String, employeeId: Int): UIO[String]
}

// front-facing API (Accessor Methods)
object CarController {
  def register(id: Int, brand: String, model: String, employeeId: Int): URIO[CarController, String] =
    ZIO.serviceWithZIO[CarController](_.register(id, brand, model, employeeId))
}

//  Implement service
case class CarControllerLive(carService: CarService) extends CarController {
  def register(id: Int, brand: String, model: String, employeeId: Int): UIO[String] = {
    Car(id, brand, model, employeeId)
      .pipe(carService.register)
      .as("Car registered successfully")
      .catchAll{
        case dbErr: DatabaseError => dbErr match {
          case ConnectionNotAvailable => ZIO
            .logError("An Database error occurred: Connection not available")
            .as("InternalServerError: An error occurred")
        }
        case serviceErr: ServiceError => serviceErr match {
          case e: EntityExistsError =>
            ZIO
              .logError(s"License plate ${e.id} already exists")
              .as(s"BadRequest: License plate ${e.id} already exists")
        }
      }
  }
}

// lift the service implementation into the ZLayer
object CarControllerLive {
  val live: URLayer[CarService, CarController] = ZLayer.fromFunction(CarControllerLive(_))
  // same as:
  val live2: URLayer[CarService, CarController] =
    ZLayer {
      for {
        carService <- ZIO.service[CarService]
      } yield CarControllerLive(carService)
    }
  // same as:
  val live3: URLayer[CarService, CarController]  =
    ZLayer(ZIO.service[CarService].map(CarControllerLive(_)))
}