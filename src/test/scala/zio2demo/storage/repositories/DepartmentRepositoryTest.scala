package zio2demo.storage.repositories

import zio.{Scope, ZIO, IO, ZLayer, ULayer, Cause}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.applicative._
import java.util.UUID

object DepartmentRepositoryTest extends ZIOSpecDefault {
  import zio2demo.model.ApplicationError.{ApplicationError, NotFound, BadRequest}
  import zio2demo.model.{Entity, EntityType, Department, Company}
  import zio2demo.storage.driver.Connection

  val uuid_company = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))
  val uuid_1 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
  val uuid_2 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
  val uuid_3 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"))
  val uuid_err = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))

  val company = Company(uuid_company, "Company s.r.o")
  val department = Department(uuid_1, "Dev", company.id)
  val department_err = Department(uuid_err, "Dev", company.id)

  case class ConnectionMock(id: String) extends Connection {
    def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      value.id match
        case id: UUID if id.compareTo(department.id) == 0 => ZIO.succeed[Unit](())
        case _ => ZIO.fail[ApplicationError](BadRequest(s"Bad request has been sent!"))

    def get[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
      uuid match
        case id: UUID if id.compareTo(department.id) == 0 => ZIO.succeed[Option[E]](department.asInstanceOf[E].pure[Option])
        case id: UUID if id.compareTo(uuid_2) == 0 => ZIO.succeed[Option[E]](None)
        case id: UUID if id.compareTo(uuid_3) == 0 => ZIO.fail[ApplicationError](NotFound(s"Department with id '${id.toString()}' has not been found! "))
        case _ => ZIO.fail[ApplicationError](BadRequest(s"Bad request has been sent!"))

    def getAll[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Vector[E]] =
      ZIO.succeed[Vector[E]](Vector[E](department.asInstanceOf[E]))

    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      ZIO.succeed[Unit](())
  }

  object ConnectionMock {
    val layer: ULayer[Connection] = ZLayer.succeed(ConnectionMock("connection-1"))
  }

  def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("Department Repository"){
    suite("Exists")(
      suite("Happy Path")(
        for {
          result <- ZIO.service[DepartmentRepository].flatMap(repository =>
              repository.exists(uuid_1) <*> repository.exists(uuid = uuid_2 ) <*> repository.exists(uuid = uuid_3 )
            )
        } yield zio.Chunk(
          test("Should return 'true', if department exists"){assert(result._1)(isTrue)},
          test("Should return 'false', if department not exists"){assert(result._2)(isFalse)},
          test("Should return 'false', if department not exists"){assert(result._3)(isFalse)}
        )
      ),

      suite("Error Path")(
        for {
          result <- ZIO.service[DepartmentRepository].flatMap(_.exists(uuid_err)).exit
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
          result <- ZIO.service[DepartmentRepository].flatMap(_.insert(department))
        } yield zio.Chunk(
          test("Should return 'Unit', if department inserted"){assert(result)(isUnit)}
        )
      ),

      suite("Error Path")(
        for {
          result <- ZIO.service[DepartmentRepository].flatMap(_.insert(department_err)).exit
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
          result <- ZIO.service[DepartmentRepository].flatMap(_.get(department.id))
        } yield zio.Chunk(
          test("Should return exact department"){assert(result)(equalTo(department))},
        )
      ),

      suite("Error Path")(
        for {
          result_1 <- ZIO.service[DepartmentRepository].flatMap(_.get(uuid_2)).exit
          result_2 <- ZIO.service[DepartmentRepository].flatMap(_.get(uuid_3)).exit
        } yield zio.Chunk(
          test("Should fail with exact error"){assert(result_1)(fails[ApplicationError](equalTo(NotFound(s"Department with id ${uuid_2} not found!"))))},
          test("Should fail with exact error"){assert(result_2)(fails[ApplicationError](equalTo(NotFound(s"Department with id '${uuid_3.toString()}' has not been found! "))))},
          // or
          test("Should contains exact cause"){assert(result_1)(failsCause[ApplicationError](equalTo(Cause.fail(NotFound(s"Department with id ${uuid_2} not found!")))))},
          test("Should contains exact cause"){assert(result_2)(failsCause[ApplicationError](equalTo(Cause.fail(NotFound(s"Department with id '${uuid_3.toString()}' has not been found! ")))))}
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
  }.provideLayer(DepartmentRepositoryLive.live  ++ ConnectionMock.layer)

}
