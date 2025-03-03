package zio2demo.storage.driver

import zio.{Tag, TagKK, Ref, ZIO, UIO, IO, URLayer, ULayer, ZLayer}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.applicative._

object ConnectionSpec extends ZIOSpecDefault {
  import zio2demo.model.ApplicationError.ApplicationError
  import zio2demo.model.{ Entity, EntityType }
  import zio2demo.model.Company
  import zio2demo.storage.driver.KeyValueStore

  val company_1 = Company(
    UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
    "Company 1",
  )

  val company_2 = Company(
    UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")),
    "Company 2",
  )

  case class KeyValueStoreMock(ref: Ref[Map[String, Vector[Entity | UUIDv7 | Unit]]]) extends KeyValueStore[ApplicationError, IO] {
    def get[V <: Entity](uuid: UUIDv7)(using entity: EntityType[V]): IO[ApplicationError, Option[V]] =
      ref.update(_.updatedWith("get") {
        case None => Vector[UUIDv7](uuid).pure[Option]
        case Some(uuids) => (uuids :+ uuid).pure[Option]
      }) *> ZIO.succeed[Option[V]](company_1.asInstanceOf[V].pure[Option])

    def getAll[V <: Entity](using entity: EntityType[V]): IO[ApplicationError, Vector[V]] =
      ref.update(_.updatedWith("getall") {
        case None => Vector[Unit](()).pure[Option]
        case Some(units) => (units :+ ()).pure[Option]
      }) *> ZIO.succeed[Vector[V]](Vector(company_1.asInstanceOf[V], company_2.asInstanceOf[V]))

    def add[V <: Entity](value: V)(using entity: EntityType[V]): IO[ApplicationError, Unit] =
      ref.update(_.updatedWith("add") {
        case None => Vector[V](value).pure[Option]
        case Some(values) => (values :+ value).pure[Option]
      }) *> ZIO.succeed[Unit](())

    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      ref.update(_.updatedWith("remove") {
        case None => Vector[UUIDv7](uuid).pure[Option]
        case Some(uuids) => (uuids :+ uuid).pure[Option]
      }) *> ZIO.succeed[Unit](())
  }

  def spec = suite("Connection") {
    zio.Chunk(
      test("Should get value from store"){
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.get[Company](company_1.id)
          calls <- cnt.get.map(_.get("get").getOrElse(Vector[UUIDv7]()))
        } yield assert(r)(isSome(equalTo(company_1))) &&     // return value is Some(company_1)
                assert(calls)(hasSize(equalTo(1))) &&        // store.get has been called once
                assert(calls.head)(equalTo(company_1.id))    // store.get has been called with id of company_1
      },

      test("Should getAll values from store"){
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.getAll[Company]
          calls <- cnt.get.map(_.get("getall").getOrElse(Vector[Unit]()))
        } yield assert(r)(hasSize(equalTo(2))) &&                             // return value is Vector with exact size
                assert(r)(hasSameElements(Vector(company_1, company_2))) &&   // return value is Vector with exact values
                assert(calls)(hasSize(equalTo(1)))                            // store.getAll has been called once
      },

      test("Should add value into store"){
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.add[Company](company_1)
          calls <- cnt.get.map(_.get("add").getOrElse(Vector[Company]()))
        } yield assert(r)(isUnit) &&                      // return value is Unit
                assert(calls)(hasSize(equalTo(1))) &&     // store.add has been called once
                assert(calls.head)(equalTo(company_1))    // store.add has been called with company_1
      },

      test("Should remove value into store"){
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.remove[Company](company_1.id)
          calls <- cnt.get.map(_.get("remove").getOrElse(Vector[UUIDv7]()))
        } yield assert(r)(isUnit) &&                          // return value is Unit
                assert(calls)(hasSize(equalTo(1))) &&         // store.remove has been called once
                assert(calls.head)(equalTo(company_1.id))    // store.remove has been called with id of company_1
      }
    )
  }
}