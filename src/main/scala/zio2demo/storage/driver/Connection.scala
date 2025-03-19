package zio2demo.storage.driver

import zio.IO
import zio.uuid.types.UUIDv7
import scala.reflect.ClassTag

import zio2demo.model.ApplicationError._
import zio2demo.model.{Entity, EntityType}

trait Connection {
  val id: String
  def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit]
  // More efficient but unsafe. This is dangerous if data is not strictly controlled. Can fail in runtime
  def getUnsafe[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] // More efficient but unsafe. This is dangerous if data is not strictly controlled. Can fail in runtime
  // Safe but unefficient
  def get[E <: Entity: ClassTag](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] // Safe but unefficient
  // More efficient but unsafe. This is dangerous if data is not strictly controlled. Can fail in runtime
  def getAllUnsafe[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] // More efficient but unsafe. This is dangerous if data is not strictly controlled. Can fail in runtime
  // Safe but unefficient
  def getAll[E <: Entity: ClassTag](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] // Safe but unefficient
  def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit]
}

case class ConnectionLive(id: String, store: KeyValueStore[ApplicationError, IO]) extends Connection {
  def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
    store.add[E](value)

  def getUnsafe[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
    store.get[E](uuid).map(_.map(_.asInstanceOf[E]))

  def get[E <: Entity: ClassTag](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
    store.get[E](uuid).map(_.collect {
      case value: E => value
    })

  def getAllUnsafe[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] =
    store.getAll[E].map(_.asInstanceOf[Seq[E]])

  def getAll[E <: Entity: ClassTag](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] =
    store.getAll[E].map(_.collect{
        case value: E => value
    })

  def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
    store.remove[E](uuid)
}
