package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio2demo.model.Car
import zio2demo.storage.repositories.CarRepository
import zio2demo.storage.Database
import zio2demo.model.error.{EntityExistsError, ServiceError, DatabaseError}

trait CarService {
  def register(car: Car): IO[DatabaseError | ServiceError, Unit]
}

object CarService {
  def register(car: Car): ZIO[CarService, DatabaseError | ServiceError, Unit] =
    ZIO.serviceWith[CarService](_.register(car))
}

case class CarServiceLive(carRepository: CarRepository, db: Database) extends CarService {
  def register(car: Car): IO[DatabaseError | ServiceError, Unit] =
    db.transact(
      carRepository.exists(car.id)
        .flatMap{
          case true  => ZIO.fail(EntityExistsError(car.id))
          case false => carRepository.insert(car)
        }
    )
}

object CarServiceLive {
  val live: URLayer[CarRepository & Database, CarService] = ZLayer.fromFunction(CarServiceLive(_, _))
  // same as:
  val live2: URLayer[CarRepository & Database, CarService] =
    ZLayer {
      for {
        carRepository <- ZIO.service[CarRepository]
        db <- ZIO.service[Database]
      } yield CarServiceLive(carRepository, db)
    }
  // same as:
  val live3: URLayer[CarRepository & Database, CarService]  = ZLayer{
    ZIO.service[CarRepository].zipWith(ZIO.service[Database])(CarServiceLive(_, _))
  }
  // same as:
  val live4: URLayer[CarRepository & Database, CarService] = ZLayer{
    (ZIO.service[CarRepository] <*> ZIO.service[Database]).map(CarServiceLive(_, _))
  }
}