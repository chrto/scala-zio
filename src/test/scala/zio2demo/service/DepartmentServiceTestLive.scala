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

object DepartmentServiceTestLive extends ZIOSpecDefault {
  import zio2demo.storage.driver.{Connection}
  import zio2demo.storage.repositories.{DepartmentRepository, DepartmentRepositoryLive}
  import zio2demo.storage.Database
  import zio2demo.model.{EntityType, Entity, ApplicationError, Company, Department}
  import zio2demo.model.ApplicationError._

  val uuid_company = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))
  val uuid_ok = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
  val uuid_new = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))

  val company = Company(uuid_company, "Department s.r.o")
  val department_ok = Department(uuid_ok, "Dev", company.id)
  val department_new = Department(uuid_new, "Dev", company.id)
  object DatabaseMock {
    val layer: ULayer[Database] =
      ZLayer.fromZIO(Ref.make(
        Map[String, Map[UUIDv7, Entity]](
          ("departments", Map[UUIDv7, Entity](department_ok.id -> department_ok)),
          ("companies", Map[UUIDv7, Entity](company.id -> company)),
        )).map(KeyValueStoreLive(_)))
        >>> ZLayer.fromFunction((kvl: KeyValueStore[ApplicationError, IO]) => Vector(
          ConnectionLive("connection-1", kvl),
          ConnectionLive("connection-2", kvl),
          ConnectionLive("connection-3", kvl)
        ))
        .flatMap((env: zio.ZEnvironment[Vector[Connection]]) => ZLayer.fromZIO(Ref.make(env.get[Vector[Connection]])))
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
        }
      )}
    )

    suite("delete")(test("..."){assertTrue(true)})
  }.provideLayer((DepartmentRepositoryLive.live ++ DatabaseMock.layer) >>> DepartmentServiceLive.live)
}
