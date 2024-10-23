package zio2demo.storage.driver

import zio.IO
import zio2demo.model.error.DatabaseError
import zio2demo.model.{Entity, EntityType}

case class Connection(id: String, store: KeyValueStore[DatabaseError, IO]) {
  def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[DatabaseError, Unit] =
    store.add[E](value)

  def get[E <: Entity](id: Int)(using entity: EntityType[E]): IO[DatabaseError, Option[Entity]] =
    store.get[E](id)

  def getAll[E <: Entity](using entity: EntityType[E]): IO[DatabaseError, Vector[Entity]] =
    store.getAll[E]

  def remove[E <: Entity](id: Int)(using entity: EntityType[E]): IO[DatabaseError, Unit] =
    store.remove[E](id)
}
