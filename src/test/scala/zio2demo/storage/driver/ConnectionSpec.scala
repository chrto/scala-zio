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

  def spec = suiteAll("Connection") {
      suite("Get value")(
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.get[Company](company_1.id)
          calls <- cnt.get.map(_.get("get").getOrElse(Vector[UUIDv7]()))
        } yield zio.Chunk(
          suite("SPEC")(
            test("store.get has been called once"){assert(calls)(hasSize(equalTo(1)))},
            test("store.get has been called with exact id"){assert(calls.head)(equalTo(company_1.id))}
          ),
          suite("TEST")(
            test("Should return Option value with exact company."){assert(r)(isSome(equalTo(company_1)))}
          )
        )
      )

      suite("Get all values")(
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.getAll[Company]
          calls <- cnt.get.map(_.get("getall").getOrElse(Vector[Unit]()))
        } yield zio.Chunk(
          suite("SPCEC")(
            test("store.getAll has been called once")(assert(calls)(hasSize(equalTo(1))))
          ),
          suite("TEST")(
            test("Should return Vector with exact size")(assert(r)(hasSize(equalTo(2)))),
            test("Should return Vector with exact values")(assert(r)(hasSameElements(Vector(company_1, company_2))))
          )
        )
      )

      suite("Add value")(
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.add[Company](company_1)
          calls <- cnt.get.map(_.get("add").getOrElse(Vector[Company]()))
        } yield zio.Chunk(
          suite("SPCEC")(
            test("store.add has been called once")(assert(calls)(hasSize(equalTo(1)))),
            test("store.add has been called with exact company"){assert(calls.head)(equalTo(company_1))}
          ),
          suite("TEST")(
            test("Should return Unit")(assert(r)(isUnit))
          )
        )
      )

      suite("Remove value")(
        for {
          cnt <- Ref.make(Map.empty[String, Vector[Entity | UUIDv7 | Unit]])
          connection <- ZIO.service[Connection].provideLayer(ZLayer.succeed(KeyValueStoreMock(cnt)) >>> ZLayer.fromFunction(ConnectionLive("connection_1", _)))
          r <- connection.remove[Company](company_1.id)
          calls <- cnt.get.map(_.get("remove").getOrElse(Vector[UUIDv7]()))
        } yield zio.Chunk(
          suite("SPCEC")(
            test("store.remove has been called once")(assert(calls)(hasSize(equalTo(1)))),
            test("store.remove has been called with exact id"){assert(calls.head)(equalTo(company_1.id))}
          ),
          suite("TEST")(
            test("Should return Unit")(assert(r)(isUnit))
          )
        )
      )
  }
}