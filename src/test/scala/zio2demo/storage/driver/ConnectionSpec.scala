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

  class KeyValueStoreMock(cnt: Counter) extends KeyValueStore[ApplicationError, IO] {
    def get[V <: Entity](uuid: UUIDv7)(using entity: EntityType[V]): IO[ApplicationError, Option[V]] =
      cnt.updateGet(uuid) *> ZIO.succeed[Option[V]](company_1.asInstanceOf[V].pure[Option])

    def getAll[V <: Entity](using entity: EntityType[V]): IO[ApplicationError, Vector[V]] =
      cnt.updateGetAll() *> ZIO.succeed[Vector[V]](Vector(company_1.asInstanceOf[V], company_2.asInstanceOf[V]))

    def add[V <: Entity](value: V)(using entity: EntityType[V]): IO[ApplicationError, Unit] =
      cnt.updateAdd(value) *> ZIO.succeed[Unit](())

    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      cnt.updateRemove(uuid) *> ZIO.succeed[Unit](())
  }

  object KeyValueStoreMock {
    val layer: ZLayer[Any, Nothing, Connection] =
      Counter.layer >>>
        ZLayer.fromFunction(KeyValueStoreMock(_)) >>>
        ZLayer.fromFunction(ConnectionLive("connection_1", _))
  }

  class Counter(ref: Ref[Map[String, Vector[Entity | UUIDv7 | Unit]]]) {
    def updateGet(uuid: UUIDv7) =
      ref.update(_.updatedWith("get") {
        case None => Vector[UUIDv7](uuid).pure[Option]
        case Some(uuids) => (uuids :+ uuid).pure[Option]
      })
    def getGet() = ref.get.map(_.get("get").getOrElse(Vector[UUIDv7]()))

    def updateGetAll() =
      ref.update(_.updatedWith("getall") {
        case None => Vector[Unit](()).pure[Option]
        case Some(units) => (units :+ ()).pure[Option]
      })
    def getGetAll() = ref.get.map(_.get("getall").getOrElse(Vector[Unit]()))

    def updateAdd(value: Entity) =
      ref.update(_.updatedWith("add") {
        case None => Vector[Entity](value).pure[Option]
        case Some(values) => (values :+ value).pure[Option]
      })
    def getAdd() = ref.get.map(_.get("add").getOrElse(Vector[Entity]()))

    def updateRemove(uuid: UUIDv7) =
      ref.update(_.updatedWith("remove") {
        case None => Vector[UUIDv7](uuid).pure[Option]
        case Some(uuids) => (uuids :+ uuid).pure[Option]
      })
    def getRemove() = ref.get.map(_.get("remove").getOrElse(Vector[UUIDv7]()))
  }

  object Counter {
    val layer: ZLayer[Any, Nothing, Counter] =
      ZLayer.fromZIO(Ref.make(Map.empty[String, Vector[zio2demo.model.Entity | UUIDv7 | Unit]])) >>>
        ZLayer.fromFunction(Counter(_))
  }

  def spec = {
    suiteAll("Connection") {

      suite("Get value")(
        for {
          result <- ZIO.service[Connection].flatMap(_.get[Company](company_1.id))
          calls <- ZIO.service[Counter].flatMap(_.getGet())
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
          calls <- ZIO.service[Counter].flatMap(_.getGetAll())
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
          calls <- ZIO.service[Counter].flatMap(_.getAdd())
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
          calls <- ZIO.service[Counter].flatMap(_.getRemove())
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
  // }.provideLayer(KeyValueStoreMock.layer ++ Counter.layer)
  }.provideShared(KeyValueStoreMock.layer ++ Counter.layer)
}