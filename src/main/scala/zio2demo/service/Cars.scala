package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio2demo.model.Car
import zio2demo.storage.repositories.CarRepository
import zio2demo.storage.DB
import zio2demo.model.error.{LicensePlateExistsError, ServiceError, DatabaseError}

class CarService(carRepository: CarRepository, db: DB) {
  def register(car: Car): IO[DatabaseError | ServiceError, Unit] =
    db.transact(
      carRepository.exists(car.licencePlate)
        .flatMap{
          case true  => ZIO.fail(LicensePlateExistsError(car.licencePlate))
          case false => carRepository.insert(car)
        }
    )
}

object CarService {
  lazy val live: URLayer[CarRepository & DB, CarService] = ZLayer.fromFunction(CarService(_, _))
}