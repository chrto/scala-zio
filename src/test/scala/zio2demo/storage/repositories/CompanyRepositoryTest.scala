package zio2demo.storage.repositories

import zio.{Scope, ZIO, IO, ZLayer, ULayer, Cause}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.applicative._
import java.util.UUID
import zio2demo.model.ApplicationError

object CompanyRepositoryTest extends ZIOSpecDefault {
  import zio2demo.model.ApplicationError.ApplicationError
  import zio2demo.model.ApplicationError.{NotFound, BadRequest}
  import zio2demo.model.{Entity, EntityType, Company}
  import zio2demo.storage.driver.Connection

  val uuid_1 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
  val uuid_2 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
  val uuid_3 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"))
  val uuid_err = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))

  val company = Company(uuid_1, "Company s.r.o")
  val company_err = Company(uuid_err, "CompanyErr s.r.o")

  case class ConnectionMock(id: String) extends Connection {
    def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      value.id match
        case id: UUID if id.compareTo(uuid_1) == 0 => ZIO.succeed[Unit](())
        case _ => ZIO.fail[ApplicationError](BadRequest(s"Bad request has been sent!"))

    def get[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
      uuid match
        case id: UUID if id.compareTo(uuid_1) == 0 => ZIO.succeed[Option[E]](company.asInstanceOf[E].pure[Option])
        case id: UUID if id.compareTo(uuid_2) == 0 => ZIO.succeed[Option[E]](None)
        case id: UUID if id.compareTo(uuid_3) == 0 => ZIO.fail[ApplicationError](NotFound(s"Company with id '${id.toString()}' has not been found! "))
        case _ => ZIO.fail[ApplicationError](BadRequest(s"Bad request has been sent!"))

    def getAll[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Vector[E]] =
      ZIO.succeed[Vector[E]](Vector[E](company.asInstanceOf[E]))

    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      ZIO.succeed[Unit](())
  }

  object ConnectionMock {
    val layer: ULayer[Connection] = ZLayer.succeed(ConnectionMock("connection-1"))
  }

  def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("Company Repository"){
    suite("Exists")(
      suite("Happy Path")(
        for {
          result <- ZIO.service[CompanyRepository].flatMap(repository =>
              repository.exists(uuid_1) <*> repository.exists(uuid = uuid_2 ) <*> repository.exists(uuid = uuid_3 )
            )
        } yield zio.Chunk(
          test("Should return 'true', if company exists"){assert(result._1)(isTrue)},
          test("Should return 'false', if company not exists"){assert(result._2)(isFalse)},
          test("Should return 'false', if company not exists"){assert(result._3)(isFalse)}
        )
      ),

      suite("Error Path")(
        for {
          result <- ZIO.service[CompanyRepository].flatMap(_.exists(uuid_err)).exit
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
          result <- ZIO.service[CompanyRepository].flatMap(_.insert(company ))
        } yield zio.Chunk(
          test("Should return 'Unit', if company inserted"){assert(result)(isUnit)}
        )
      ),

      suite("Error Path")(
        for {
          result <- ZIO.service[CompanyRepository].flatMap(_.insert(company_err )).exit
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
          result <- ZIO.service[CompanyRepository].flatMap(_.get(uuid_1))
        } yield zio.Chunk(
          test("Should return exact company"){assert(result)(equalTo(company))},
        )
      ),

      suite("Error Path")(
        for {
          result_1 <- ZIO.service[CompanyRepository].flatMap(_.get(uuid_2)).exit
          result_2 <- ZIO.service[CompanyRepository].flatMap(_.get(uuid_3)).exit
        } yield zio.Chunk(
          test("Should fail with exact error"){assert(result_1)(fails[ApplicationError](equalTo(NotFound(s"Company with id ${uuid_2} not found!"))))},
          test("Should fail with exact error"){assert(result_2)(fails[ApplicationError](equalTo(NotFound(s"Company with id '${uuid_3.toString()}' has not been found! "))))},
          // or
          test("Should contains exact cause"){assert(result_1)(failsCause[ApplicationError](equalTo(Cause.fail(NotFound(s"Company with id ${uuid_2} not found!")))))},
          test("Should contains exact cause"){assert(result_2)(failsCause[ApplicationError](equalTo(Cause.fail(NotFound(s"Company with id '${uuid_3.toString()}' has not been found! ")))))}
        )
      )
    )
    // etc.
    // suite("getAll")(
    //   // ..
    // )
    // suite("delete")(
    //   // ..
    // )
  }.provideLayer(CompanyRepositoryLive.live  ++ ConnectionMock.layer)

}
