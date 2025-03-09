package zio2demo.storage.driver

import zio.IO
import zio.uuid.types.UUIDv7

import zio2demo.model.ApplicationError._
import zio2demo.model.{Entity, EntityType}

trait Connection {
  val id: String
  def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit]
  def get[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]]
  def getAll[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Vector[E]]
  def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit]
}

case class ConnectionLive(id: String, store: KeyValueStore[ApplicationError, IO]) extends Connection {
  def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
    store.add[E](value)

  def get[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
    store.get[E](uuid)

  def getAll[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Vector[E]] =
    store.getAll[E]

  def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
    store.remove[E](uuid)
}
