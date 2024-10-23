package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Car
import zio2demo.model.error.{DatabaseError, NotFound}
import zio2demo.storage.driver.KeyValueStore

import scala.util.chaining._
import zio2demo.model.Entity
trait CarRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean]
  def insert(car: Car): ZIO[Connection, DatabaseError, Unit]
  def get(id: Int): ZIO[Connection, DatabaseError, Car]
  def getAll: ZIO[Connection, DatabaseError, Vector[Car]]
  def delete(id: Int): ZIO[Connection, DatabaseError, Unit]
}

case class CarRepositoryLive() extends CarRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Checking if car with id ${id} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Car](id))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Car with id ${id} exists")
        case false => ZIO.logInfo(s"Car with id ${id} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(car: Car): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Inserting car with id ${car.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Car](car))
      .tap(_ => ZIO.logInfo(s"Car with id ${car.id} inserted"))

  def get(id: Int): ZIO[Connection, DatabaseError, Car] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting car with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.get[Car](id))
      .flatMap{
        case Some(car: Car) => ZIO.succeed(car)
        case _ => ZIO.fail(NotFound(s"No car found with id ${id}"))
      }

  def getAll: ZIO[Connection, DatabaseError, Vector[Car]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting all cars using connection with id: ${c.id}"))
      .flatMap(_.getAll[Car])
      .flatMap ((cars: Vector[Entity]) => ZIO.succeed(cars.collect { case car: Car => car }))

  def delete(id: Int): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Deleting car with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.remove[Car](id))
}

object CarRepositoryLive {
  lazy val live: ULayer[CarRepository] = ZLayer.succeed(CarRepositoryLive())
}