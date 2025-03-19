package zio2demo.service

import zio.test.ZIOSpecDefault
import zio.{ZIO, ZLayer, ULayer, Ref, IO, Tag, Scope}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.all._
import zio2demo.storage.driver.ConnectionPool
import zio2demo.storage.driver.ConnectionPoolLive
import zio2demo.storage.DatabaseLive
import zio2demo.storage.driver.KeyValueStore
import zio2demo.storage.driver.KeyValueStoreLive
import zio2demo.storage.driver.ConnectionLive
import scala.reflect.ClassTag

object DepartmentServiceTest extends ZIOSpecDefault {
  import zio2demo.storage.driver.{Connection}
  import zio2demo.storage.repositories.{DepartmentRepository, DepartmentRepositoryLive}
  import zio2demo.storage.Database
  import zio2demo.model.{EntityType, Entity, ApplicationError, Company, Department}
  import zio2demo.model.ApplicationError._

  val uuid_company = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))
  val uuid_ok = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
  val uuid_new = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
  val uuid_err = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))
  val uuid_new_err = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000005"))

  val company = Company(uuid_company, "Department s.r.o")
  val department_ok = Department(uuid_ok, "Dev", company.id)
  val department_new = Department(uuid_new, "Dev", company.id)
  val department_err = Department(uuid_err, "Err", company.id)
  val department_new_err = Department(uuid_new_err, "New", company.id)

  class ConnectionMock(connectionId: String) extends Connection {
    val id = connectionId
    def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      value.id match
        case entityId if entityId.compareTo(uuid_new) == 0 => ZIO.succeed[Unit](())
        case entityId => ZIO.fail(BadRequest(s"Bad request $entityId"))

    def getUnsafe[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] = ???

    def get[E <: Entity: ClassTag](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
      uuid match
        case entityId if entityId.compareTo(uuid_ok) == 0 => ZIO.succeed[Option[E]](department_ok.asInstanceOf[E].pure[Option])
        case entityId if entityId.compareTo(uuid_new_err) == 0 => ZIO.succeed[Option[E]](None)
        case entityId if entityId.compareTo(uuid_new) == 0 => ZIO.succeed[Option[E]](None)
        case entityId => ZIO.fail(BadRequest(s"Bad request $entityId"))

    def getAllUnsafe[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] = ???

    def getAll[E <: Entity: ClassTag](using entity: EntityType[E]): IO[ApplicationError, Vector[E]] = ???
    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] = ???
  }

  object DatabaseMock {
    val layerPool: ULayer[Ref[Vector[Connection]]] =
      ZLayer.fromZIO(Ref.make(Vector(
        ConnectionMock("connection-1"),
        ConnectionMock("connection-2"),
        ConnectionMock("connection-3")
      )))

    val layer: ULayer[Database] =
      layerPool
        >>> ZLayer.fromFunction(ConnectionPoolLive(_))
        >>> ZLayer.fromFunction(DatabaseLive(_))
  }

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suiteAll("Department Service - TEST"){
    suite("get")(
      suite("Happy path") {
        test("Should return exact department"){
          ZIO.service[DepartmentService]
            .flatMap(_.get(uuid_ok))
            .map(assert(_)(equalTo(department_ok)))
        }
      },
      suite("Error path") {zio.Chunk(
        test("Should fail with 'NotFound' error, if department does not exists"){
          ZIO.service[DepartmentService]
            .flatMap(_.get(uuid_new)).exit
            .map(assert(_)(fails(equalTo(NotFound(s"Department with id ${uuid_new} not found!")))))
        },
        test("Should fail with exactly the same error, as connection failed with"){
          ZIO.service[DepartmentService]
            .flatMap(_.get(uuid_err)).exit
            .map(assert(_)(fails(equalTo(BadRequest(s"Bad request $uuid_err")))))
        }
      )}
    )

    suite("getAll")(test("..."){assertTrue(true)})

    suite("add")(
      suite("Happy path") {
        test("Should return Unit, if department has been added"){
          ZIO.service[DepartmentService]
            .flatMap(_.add(department_new))
            .map(assert(_)(isUnit))
        }
      },
      suite("Error path") {zio.Chunk(
        test("Should fail with exact 'BadRequest' error, if department already exists"){
          ZIO.service[DepartmentService]
            .flatMap(_.add(department_ok)).exit
            .map(assert(_)(fails(equalTo(BadRequest(s"Department with id ${department_ok.id} already exists!")))))
        },
        test("Should fail with exactly the same error, as connection failed with"){
          ZIO.service[DepartmentService]
            .flatMap(_.add(department_new_err)).exit
            .map(assert(_)(fails(equalTo(BadRequest(s"Bad request $uuid_new_err")))))
        }
      )}
    )

    suite("delete")(test("..."){assertTrue(true)})
  }.provideLayer((DepartmentRepositoryLive.live ++ DatabaseMock.layer) >>> DepartmentServiceLive.live)
}
