package zio2demo.storage.driver

import zio.IO
import zio.uuid.types.UUIDv7

import zio2demo.model.ApplicationError._
import zio2demo.model.{Entity, EntityType}

case class Connection(id: String, store: KeyValueStore[ApplicationError, IO]) {
  def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
    store.add[E](value)

  def get[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[Entity]] =
    store.get[E](uuid)

  def getAll[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Vector[Entity]] =
    store.getAll[E]

  def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
    store.remove[E](uuid)
}
