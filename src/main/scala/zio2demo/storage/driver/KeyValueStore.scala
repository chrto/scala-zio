package zio2demo.storage.driver

import zio.{Tag, TagKK, ZIO, Ref, IO, ULayer, ZLayer}
import zio.uuid.types.UUIDv7

import zio2demo.model.ApplicationError._
import zio2demo.model.{Entity, EntityType}

trait KeyValueStore[E, F[_, _]] {
  def get[V <: Entity](uuid: UUIDv7)(using entity: EntityType[V]): F[E, Option[V]]
  def getAll[V <: Entity](using entity: EntityType[V]): F[E, Vector[V]]
  def add[V <: Entity](value: V)(using entity: EntityType[V]): F[E, Unit]
  def remove[V <: Entity](uuid: UUIDv7)(using entity: EntityType[V]): F[E, Unit]
}

object KeyValueStore {
  def get[V <: Entity, E: Tag, F[_, _]: TagKK](uuid: UUIDv7)(using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Option[V]]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.get(uuid))

  def getAll[V <: Entity, E: Tag, F[_, _]: TagKK](using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Vector[V]]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.getAll)

  def add[V <: Entity, E: Tag, F[_, _]: TagKK](value: V)(using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Unit]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.add(value))

  def remove[V <: Entity, E: Tag, F[_, _]: TagKK](uuid: UUIDv7)(using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Unit]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.remove(uuid))
}

case class KeyValueStoreLive(memoryStorage: Ref[Map[String, Vector[Entity]]]) extends KeyValueStore[ApplicationError, IO] {
  def get[V <: Entity](uuid: UUIDv7)(using entity: EntityType[V]): IO[ApplicationError, Option[V]] =
    memoryStorage.get
      .flatMap(
        _.get(entity.entityName)
        .map(_.collectFirst[V]{
            case e if e.id == uuid => e.asInstanceOf[V]
        })
        .fold
          (ZIO.fail(NotFound(s"No storage found for ${entity.entityName}")))
          (ZIO.succeed)
      )

  def getAll[V <: Entity](using entity: EntityType[V]): IO[ApplicationError, Vector[V]] =
    memoryStorage.get
      .flatMap(
        _.get(entity.entityName)
        .map(_.asInstanceOf[Vector[V]])
        .fold
          (ZIO.fail(NotFound(s"No storage found for ${entity.entityName}")))
          (ZIO.succeed)
      )

  def add[V <: Entity](value: V)(using entity: EntityType[V]): IO[ApplicationError, Unit] =
    memoryStorage.update(_.updatedWith(entity.entityName){
      case None => Some(Vector[V](value))
      case Some(values) => Some(values :+ value)
    })

  def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
    memoryStorage.update(_.updatedWith(entity.entityName){
      case None => None
      case Some(values) => Some(values.filterNot(_.id == uuid))
    })
}

object KeyValueStoreLive {
  import zio2demo.model.{Company}
  val live: ULayer[KeyValueStore[ApplicationError, IO]] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, Vector[Entity]]).map(KeyValueStoreLive(_)))
}
