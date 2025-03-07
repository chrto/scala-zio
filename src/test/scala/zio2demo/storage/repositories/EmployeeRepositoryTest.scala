package zio2demo.storage.repositories

import zio.{Scope, ZIO, IO, ZLayer, ULayer, Cause}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.applicative._
import java.util.UUID

object EmployeeRepositoryTest extends ZIOSpecDefault {
  import zio2demo.model.ApplicationError.{ApplicationError, NotFound, BadRequest}
  import zio2demo.model.{Entity, EntityType, Company, Department, Employee}
  import zio2demo.storage.driver.Connection

  val uuid_company = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))
  val uuid_department = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
  val uuid_1 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
  val uuid_2 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"))
  val uuid_3 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))
  val uuid_err = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000005"))

  val company = Company(uuid_company, "Company s.r.o")
  val department = Department(uuid_department, "Dev", company.id)
  val employee = Employee(uuid_1, "Joe Doe", "joe.doe@company.com", "s3cr3t", department.id)
  val employee_err = Employee(uuid_err, "Jack Black", "jack.black@company.com", "pwd", department.id)

  case class ConnectionMock(id: String) extends Connection {
    def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      value.id match
        case id: UUID if id.compareTo(employee.id) == 0 => ZIO.succeed[Unit](())
        case _ => ZIO.fail[ApplicationError](BadRequest(s"Bad request has been sent!"))

    def get[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
      uuid match
        case id: UUID if id.compareTo(employee.id) == 0 => ZIO.succeed[Option[E]](employee.asInstanceOf[E].pure[Option])
        case id: UUID if id.compareTo(uuid_2) == 0 => ZIO.succeed[Option[E]](None)
        case id: UUID if id.compareTo(uuid_3) == 0 => ZIO.fail[ApplicationError](NotFound(s"Employee with id '${id.toString()}' has not been found! "))
        case _ => ZIO.fail[ApplicationError](BadRequest(s"Bad request has been sent!"))

    def getAll[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Vector[E]] =
      ZIO.succeed[Vector[E]](Vector[E](employee.asInstanceOf[E]))

    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      ZIO.succeed[Unit](())
  }

  object ConnectionMock {
    val layer = ZLayer.succeed(ConnectionMock("connection-1"))
  }

  def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("Employee Repository"){
    suite("Exists")(
      suite("Happy Path")(
        for {
          result <- ZIO.service[EmployeeRepository].flatMap(repository =>
              repository.exists(uuid_1) <*> repository.exists(uuid_2) <*> repository.exists(uuid_3)
            )
        } yield zio.Chunk(
          test("Should return 'true', if employee exists"){assert(result._1)(isTrue)},
          test("Should return 'false', if employee not exists"){assert(result._2)(isFalse)},
          test("Should return 'false', if employee not exists"){assert(result._3)(isFalse)}
        )
      ),

      suite("Error Path")(
        for {
          result <- ZIO.service[EmployeeRepository].flatMap(_.exists(uuid_err)).exit
        } yield zio.Chunk(
          test("Should fail with exact error"){assert(result)(fails[ApplicationError](equalTo(BadRequest(s"Bad request has been sent!"))))},
          // or
          test("Should contains exact cause"){assert(result)(failsCause[ApplicationError](equalTo(Cause.fail(BadRequest(s"Bad request has been sent!")))))}
        )
      )
    )

    suite("insert")(
      suite("Happy Path")(
        for {
          result <- ZIO.service[EmployeeRepository].flatMap(_.insert(employee))
        } yield zio.Chunk(
          test("Should return 'Unit', if employee inserted"){assert(result)(isUnit)}
        )
      ),

      suite("Error Path")(
        for {
          result <- ZIO.service[EmployeeRepository].flatMap(_.insert(employee_err)).exit
        } yield zio.Chunk(
          test("Should fail with exact error"){assert(result)(fails[ApplicationError](equalTo(BadRequest(s"Bad request has been sent!"))))},
          // or
          test("Should contains exact cause"){assert(result)(failsCause[ApplicationError](equalTo(Cause.fail(BadRequest(s"Bad request has been sent!")))))}
        )
      )
    )

    suite("get")(
      suite("Happy Path")(
        for {
          result <- ZIO.service[EmployeeRepository].flatMap(_.get(employee.id))
        } yield zio.Chunk(
          test("Should return exact employee"){assert(result)(equalTo(employee))},
        )
      ),

      suite("Error Path")(
        for {
          result_1 <- ZIO.service[EmployeeRepository].flatMap(_.get(uuid_2)).exit
          result_2 <- ZIO.service[EmployeeRepository].flatMap(_.get(uuid_3)).exit
        } yield zio.Chunk(
          test("Should fail with exact error"){assert(result_1)(fails[ApplicationError](equalTo(NotFound(s"Employee with id ${uuid_2} not found!"))))},
          test("Should fail with exact error"){assert(result_2)(fails[ApplicationError](equalTo(NotFound(s"Employee with id '${uuid_3.toString()}' has not been found! "))))},
          // or
          test("Should contains exact cause"){assert(result_1)(failsCause[ApplicationError](equalTo(Cause.fail(NotFound(s"Employee with id ${uuid_2} not found!")))))},
          test("Should contains exact cause"){assert(result_2)(failsCause[ApplicationError](equalTo(Cause.fail(NotFound(s"Employee with id '${uuid_3.toString()}' has not been found! ")))))}
        )
      )
    )
    // etc.
    suite("getAll - not implemented yet")(
      test("...")(assertTrue(true))
      // ..
      )
      suite("delete - not implemented yet")(
      test("...")(assertTrue(true))
      // ..
    )
  }.provideLayer(EmployeeRepositoryLive.live  ++ ConnectionMock.layer)

}
