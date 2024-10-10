package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref}

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Car

class CarRepository() {
  def exists(licencePlate: String): URIO[Connection, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Checking if car with licence plate $licencePlate exists using connection with id: ${c.id}"))
      .flatMap(_.get(licencePlate).map(_.isDefined))
      .tap{
        case true   => ZIO.logWarning(s"Car with licence plate $licencePlate exists")
        case false  => ZIO.logInfo(s"Car with licence plate $licencePlate does not exist")
      }

  def insert(car: Car): URIO[Connection, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Inserting car with licence plate ${car.licencePlate} using connection with id: ${c.id}"))
      .flatMap(_.add(car))
      .tap(_ => ZIO.logInfo(s"Car with licence plate ${car.licencePlate} inserted"))
}

object CarRepository {
  lazy val live: ULayer[CarRepository] = ZLayer.succeed(CarRepository())
}