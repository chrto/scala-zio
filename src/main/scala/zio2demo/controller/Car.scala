package zio2demo.controll

import zio.{ZIO, URLayer, ZLayer}

import scala.util.chaining._

import zio2demo.service.CarService
import zio2demo.model.Car
import zio2demo.model.error.LicensePlateExistsError
import zio2demo.model.error.ServiceError
import zio2demo.model.error.DatabaseError
import zio2demo.model.error.ConnectionNotAvailable

class CarController(carService: CarService) {
  def registerNewCar(licencePlate: String, brand: String, model: String): ZIO[Any, Nothing, String] = {
    Car(licencePlate, brand, model)
      .pipe(carService.register)
      .as("Car registered successfully")
      .catchAll{
        case dbErr: DatabaseError => dbErr match {
          case ConnectionNotAvailable => ZIO
            .logError("An Database error occurred: Connection not available")
            .as("InternalServerError: An error occurred")
        }
        case serviceErr: ServiceError => serviceErr match {
          case e: LicensePlateExistsError =>
            ZIO
              .logError(s"License plate ${e.licencePlate} already exists")
              .as(s"BadRequest: License plate ${e.licencePlate} already exists")
        }
      }
  }
}

object CarController {
  lazy val live: URLayer[CarService, CarController] = ZLayer.fromFunction(CarController(_))
}