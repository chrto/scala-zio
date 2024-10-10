package zio2demo.storage.driver

import zio.{Tag, TagKK, ZIO, Ref, IO, ULayer, ZLayer}
import zio2demo.model.error.{DatabaseError, NotFound}
import zio2demo.model.{Entity, EntityType}

trait KeyValueStore[E, F[_, _]] {
  def get[V <: Entity](id: Int)(using entity: EntityType[V]): F[E, Option[Entity]]
  def getAll[V <: Entity](using entity: EntityType[V]): F[E, Vector[Entity]]
  def add[V <: Entity](value: V)(using entity: EntityType[V]): F[E, Unit]
  def remove[V <: Entity](id: Int)(using entity: EntityType[V]): F[E, Unit]
}

object KeyValueStore {
  def get[V <: Entity, E: Tag, F[_, _]: TagKK](id: Int)(using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Option[Entity]]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.get(id))

  def getAll[V <: Entity, E: Tag, F[_, _]: TagKK](using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Vector[Entity]]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.getAll)

  def add[V <: Entity, E: Tag, F[_, _]: TagKK](value: V)(using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Unit]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.add(value))

  def remove[V <: Entity, E: Tag, F[_, _]: TagKK](id: Int)(using entity: EntityType[V]): ZIO[KeyValueStore[E, F], Nothing, F[E, Unit]] =
    ZIO.serviceWith[KeyValueStore[E, F]](_.remove(id))
}

case class KeyValueStoreLive(memoryStorage: Ref[Map[String, Vector[Entity]]]) extends KeyValueStore[DatabaseError, IO] {
  def get[V <: Entity](id: Int)(using entity: EntityType[V]): IO[DatabaseError, Option[Entity]] =
    memoryStorage.get
      .flatMap(
        _.get(entity.entityName)
        .map(_.collectFirst{
            case e: Entity if e.id == id => e
        })
        .fold
          (ZIO.fail(NotFound(s"No storage found for ${entity.entityName}")))
          (ZIO.succeed)
      )

  def getAll[V <: Entity](using entity: EntityType[V]): IO[DatabaseError, Vector[Entity]] =
    memoryStorage.get
      .flatMap(
        _.get(entity.entityName)
        .fold
          (ZIO.fail(NotFound(s"No storage found for ${entity.entityName}")))
          (ZIO.succeed)
      )

  def add[V <: Entity](value: V)(using entity: EntityType[V]): IO[DatabaseError, Unit] =
    memoryStorage.update(_.updatedWith(entity.entityName){
      case None => Some(Vector[V](value))
      case Some(values) => Some(values :+ value)
    })

  def remove[E <: Entity](id: Int)(using entity: EntityType[E]): IO[DatabaseError, Unit] =
    memoryStorage.update(_.updatedWith(entity.entityName){
      case None => None
      case Some(values) => Some(values.filterNot(_.id == id))
    })
}

object KeyValueStoreLive {
  val live: ULayer[KeyValueStore[DatabaseError, IO]] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, Vector[Entity]]).map(KeyValueStoreLive(_)))
}
