package zio2demo.storage.driver

import zio.{ZIO, UIO, Ref, ZLayer, ULayer, URIO}
import zio2demo.model.Car
import scala.collection.immutable.Vector

case class Connection(id: String, repository: Ref[Vector[Car]]) {
  def add(car: Car): UIO[Unit] =
    repository.update(_ :+ car)

  def get(licencePlate: String): UIO[Option[Car]] =
    repository.modify(cars => (cars.find(_.licencePlate == licencePlate), cars))

  def getAll(): UIO[Vector[Car]] =
    repository.get
}
