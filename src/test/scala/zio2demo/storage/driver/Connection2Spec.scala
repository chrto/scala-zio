package zio2demo.storage.driver

import zio.{Scope, Tag, TagKK, Ref, ZIO, UIO, IO, URLayer, ULayer, ZLayer}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.applicative._

object Connection2Spec extends ZIOSpecDefault {
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

  class KeyValueStoreMock(ref: Ref[Vector[Entity | UUIDv7 | Unit]]) extends KeyValueStore[ApplicationError, IO] {
    def get[V <: Entity](uuid: UUIDv7)(using entity: EntityType[V]): IO[ApplicationError, Option[V]] =
      ref.update(_ :+ uuid) *> ZIO.succeed[Option[V]](company_1.asInstanceOf[V].pure[Option])

    def getAll[V <: Entity](using entity: EntityType[V]): IO[ApplicationError, Vector[V]] =
      ref.update(_ :+ ()) *> ZIO.succeed[Vector[V]](Vector(company_1.asInstanceOf[V], company_2.asInstanceOf[V]))

    def add[V <: Entity](value: V)(using entity: EntityType[V]): IO[ApplicationError, Unit] =
      ref.update(_ :+ value) *> ZIO.succeed[Unit](())

    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      ref.update(_ :+ uuid) *> ZIO.succeed[Unit](())
  }

  object RefMock {
    val layer: ZLayer[Any, Nothing, Ref[Vector[Entity | UUIDv7 | Unit]]] = ZLayer.fromZIO(Ref.make(Vector.empty[Entity | UUIDv7 | Unit]))
  }

  object KeyValueStoreMock {
    val layer: ZLayer[Any, Nothing, ConnectionLive] =
      RefMock.layer >>>
        ZLayer.fromFunction(KeyValueStoreMock(_)) >>>
        ZLayer.fromFunction(ConnectionLive("connection_1", _))
  }

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = {
    suiteAll("Connection") {

      suite("Get value")(
        for {
          result <- ZIO.service[Connection].flatMap(_.get[Company](company_1.id))
          calls <- ZIO.service[Ref[Vector[Entity | UUIDv7 | Unit]]].flatMap(_.get)
        } yield zio.Chunk(
          suite("SPEC")(
            test("store.get has been called once"){assert(calls)(hasSize(equalTo(1)))},
            test("store.get has been called with exact id"){assert(calls.head)(equalTo(company_1.id))}
          ),
          suite("TEST")(
            test("Should return Option value with exact company."){assert(result)(isSome(equalTo(company_1)))}
          )
        )
      )

      suite("Get all values")(
        for {
          result <- ZIO.service[Connection].flatMap(_.getAll[Company])
          calls <- ZIO.service[Ref[Vector[Entity | UUIDv7 | Unit]]].flatMap(_.get)
        } yield zio.Chunk(
          suite("SPEC")(
            test("store.getAll has been called once")(assert(calls)(hasSize(equalTo(1))))
          ),
          suite("TEST")(
            test("Should return Vector with exact size")(assert(result)(hasSize(equalTo(2)))),
            test("Should return Vector with exact values")(assert(result)(hasSameElements(Vector(company_1, company_2))))
          )
        )
      )

      suite("Add value")(
        for {
          result <- ZIO.service[Connection].flatMap(_.add[Company](company_1))
          calls <- ZIO.service[Ref[Vector[Entity | UUIDv7 | Unit]]].flatMap(_.get)
        } yield zio.Chunk(
          suite("SPEC")(
            test("store.add has been called once")(assert(calls)(hasSize(equalTo(1)))),
            test("store.add has been called with exact company"){assert(calls.head)(equalTo(company_1))}
          ),
          suite("TEST")(
            test("Should return Unit")(assert(result)(isUnit))
          )
        )
      )

      suite("Remove value")(
        for {
          result <- ZIO.service[Connection].flatMap(_.remove[Company](company_1.id))
          calls <- ZIO.service[Ref[Vector[Entity | UUIDv7 | Unit]]].flatMap(_.get)
       } yield zio.Chunk(
          suite("SPEC")(
            test("store.remove has been called once")(assert(calls)(hasSize(equalTo(1)))),
            test("store.remove has been called with exact id"){assert(calls.head)(equalTo(company_1.id))}
          ),
          suite("TEST")(
            test("Should return Unit")(assert(result)(isUnit))
          )
        )
      )
      }
  }.provideLayer(KeyValueStoreMock.layer ++ RefMock.layer)
}