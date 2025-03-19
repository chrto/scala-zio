package zio2demo.service

import zio.test.ZIOSpecDefault
import zio.{ZIO, ZLayer, ULayer, Ref, IO, Tag}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.all._
import scala.reflect.ClassTag

object CompanyServiceSpec extends ZIOSpecDefault {
  import zio2demo.storage.driver.{Connection}
  import zio2demo.storage.repositories.{CompanyRepository, CompanyRepositoryLive}
  import zio2demo.storage.Database
  import zio2demo.model.{EntityType, Entity, ApplicationError, Company}
  import zio2demo.model.ApplicationError._

  val uuid_ok = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
  val uuid_new = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
  val uuid_err = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))
  val uuid_new_err = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000005"))

  val company = Company(uuid_ok, "Company s.r.o")
  val company_new = Company(uuid_new, "New s.r.o")
  val company_err = Company(uuid_err, "CompanyErr s.r.o")
  val company_new_err = Company(uuid_new_err, "CompanyNewErr s.r.o")

  object CounterMock {
    val layer: ZLayer[Any, Nothing, Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]] =
      ZLayer.fromZIO(Ref.make(Vector.empty[(String, Option[Any], Option[Either[ApplicationError, Any]])]))
  }

  class CompanyRepositoryMock(companyRepositoryLive: CompanyRepository, ref: Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]) extends CompanyRepository {
    private def updateRef[I, O](name: String, in: I, result: ZIO[Connection, ApplicationError, O]): ZIO[Connection, ApplicationError, O] =
      result.tapBoth(
        (failure: ApplicationError) => ref.update(_ :+ (name, in.pure[Option], failure.asLeft[O].pure[Option])),
        (value: O)                  => ref.update(_ :+ (name, in.pure[Option], value.asRight[ApplicationError].pure[Option]))
      )

    def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit] =
      this.updateRef[UUIDv7, Unit]("delete", uuid, companyRepositoryLive.delete(uuid))

    def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean] =
      this.updateRef[UUIDv7, Boolean]("exists", uuid, companyRepositoryLive.exists(uuid))

    def getUnsafe(uuid: UUIDv7): ZIO[Connection, ApplicationError, Company] =
      this.updateRef[UUIDv7, Company]("getUnsafe", uuid, companyRepositoryLive.getUnsafe(uuid))

    def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Company] =
      this.updateRef[UUIDv7, Company]("get", uuid, companyRepositoryLive.get(uuid))

    def getAllUnsafe: ZIO[Connection, ApplicationError, Seq[Company]] =
      this.updateRef[Unit, Seq[Company]]("getAllUnsafe", (), companyRepositoryLive.getAllUnsafe)

    def getAll: ZIO[Connection, ApplicationError, Seq[Company]] =
      this.updateRef[Unit, Seq[Company]]("getAll", (), companyRepositoryLive.getAll)

    def insert(company: Company): ZIO[Connection, ApplicationError, Unit] =
      this.updateRef[Company, Unit]("insert", company, companyRepositoryLive.insert(company))
  }

  object CompanyRepositoryMock {
    val layer: ULayer[CompanyRepository] = CounterMock.layer >>> ZLayer.fromFunction(CompanyRepositoryMock(CompanyRepositoryLive() ,_))
  }

  class ConnectionMock() extends Connection {
    val id: String = ConnectionMock.id
    def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] =
      value.id match
        case id if id.compareTo(uuid_new) == 0 => ZIO.succeed[Unit](())
        case id => ZIO.fail(BadRequest(s"Bad request $id"))

    def getUnsafe[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] = ???
    def get[E <: Entity: ClassTag](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
      uuid match
        case id if id.compareTo(uuid_ok) == 0 => ZIO.succeed[Option[E]](company.asInstanceOf[E].pure[Option])
        case id if id.compareTo(uuid_new_err) == 0 => ZIO.succeed[Option[E]](None)
        case id if id.compareTo(uuid_new) == 0 => ZIO.succeed[Option[E]](None)
        case id => ZIO.fail(BadRequest(s"Bad request $id"))

    def getAllUnsafe[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] = ???
    def getAll[E <: Entity: ClassTag](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] = ???
    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] = ???
  }

  object ConnectionMock {
    val id: String = "connectionMockId"
    val layer: ULayer[ConnectionMock] = ZLayer.succeed(ConnectionMock())
  }

  class DatabaseMock(ref: Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]) extends Database {
    def transact[R, A: Tag](dbProgram: ZIO[Connection & R, ApplicationError, A]): ZIO[R, ApplicationError, A] =
      ref.update(_ :+ ("transact", None, None)) *>
      dbProgram.provideSomeLayer[R](ConnectionMock.layer)
  }

  object DatabaseMock {
    val layer: ULayer[Database] = CounterMock.layer >>> ZLayer.fromFunction(DatabaseMock(_))
  }


  def spec = suiteAll("Company Service"){
    suite("get")(
      suite("Happy path") {
        for {
          result <- ZIO.service[CompanyService].flatMap(_.get(uuid_ok))
          cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
        } yield zio.Chunk(
          suite("TEST")(
            test("Should return exact company"){assert(result)(equalTo(company))}
          ),
          suite("SPEC")(
            test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
            test("Should call CompanyRepository.get once"){assert(cnt.filter(_._1 == "get"))(hasSize(equalTo(1)))},
            test("Should call CompanyRepository.get with exact id"){assert(cnt.find(_._1 == "get").map(_._2).flatten)(isSome(equalTo(uuid_ok)))},
            test("Should return exact value - CompanyRepository.get"){assert(cnt.find(_._1 == "get").map(_._3).flatten)(isSome(isRight(equalTo(company))))}
          )
        )
      },
      suite("Error path") {zio.Chunk(
        suite("Not Found")(
          for {
            result <- ZIO.service[CompanyService].flatMap(_.get(uuid_new)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
          } yield zio.Chunk(
            suite("TEST")(
              test("Should fail with exact error"){assert(result)(fails(equalTo(NotFound(s"Company with id ${uuid_new} not found!"))))}
            ),
            suite("SPEC")(
              test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
              test("Should call CompanyRepository.get once"){assert(cnt.filter(_._1 == "get"))(hasSize(equalTo(1)))},
              test("Should call CompanyRepository.get with exact id"){assert(cnt.find(_._1 == "get").map(_._2).flatten)(isSome(equalTo(uuid_new)))},
              test("Should fail with exact error - CompanyRepository.get"){assert(cnt.find(_._1 == "get").map(_._3).flatten)(isSome(isLeft(equalTo(NotFound(s"Company with id ${uuid_new} not found!")))))}
            )
          )
        ),
        suite("Failed"){
          for {
            result <- ZIO.service[CompanyService].flatMap(_.get(uuid_err)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
          } yield zio.Chunk(
            suite("TEST")(
              test("Should fail with exact error"){assert(result)(fails(equalTo(BadRequest(s"Bad request $uuid_err"))))}
            ),
            suite("SPEC")(
              test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
              test("Should call CompanyRepository.get once"){assert(cnt.filter(_._1 == "get"))(hasSize(equalTo(1)))},
              test("Should call CompanyRepository.get with exact id"){assert(cnt.find(_._1 == "get").map(_._2).flatten)(isSome(equalTo(uuid_err)))},
              test("Should fail with exact error - CompanyRepository.get"){assert(cnt.find(_._1 == "get").map(_._3).flatten)(isSome(isLeft(equalTo(BadRequest(s"Bad request $uuid_err")))))}
            )
          )
        }
      )}
    )

    suite("getAll")(test("..."){assertTrue(true)})

    suite("add")(
      suite("Happy path")(
        for {
          result <- ZIO.service[CompanyService].flatMap(_.add(company_new))
          cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
        } yield zio.Chunk(
          suite("TEST")(
            test("Should return Unit"){assert(result)(isUnit)}
          ),
          suite("SPEC")(
            test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
            test("Should call CompanyRepository.exists once"){assert(cnt.filter(_._1 == "exists"))(hasSize(equalTo(1)))},
            test("Should call CompanyRepository.exists with exact id"){assert(cnt.find(_._1 == "exists").map(_._2).flatten)(isSome(equalTo(uuid_new)))},
            test("Should return exact value - CompanyRepository.exists"){assert(cnt.find(_._1 == "exists").map(_._3).flatten)(isSome(isRight(equalTo(false))))},
            test("Should call CompanyRepository.insert once"){assert(cnt.filter(_._1 == "insert"))(hasSize(equalTo(1)))},
            test("Should call CompanyRepository.insert with exact value"){assert(cnt.find(_._1 == "insert").map(_._2).flatten)(isSome(equalTo(company_new)))},
            test("Should return exact value - CompanyRepository.insert"){assert(cnt.find(_._1 == "insert").map(_._3).flatten)(isSome(isRight(equalTo(()))))},
            test("Should call CompanyRepository.exists before CompanyRepository.insert"){assert(cnt.map(_._1).indexOf("exists"))(isLessThan(cnt.map(_._1).indexOf("insert")))}
          )
        )
      ),

      suite("Error path")(
        suite("Entity exists")(
          for {
            result <- ZIO.service[CompanyService].flatMap(_.add(company)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
          } yield zio.Chunk(
            suite("TEST")(
              test("Should fail with exact error"){assert(result)(fails(equalTo(BadRequest(s"Company with id ${company.id} already exists!"))))}
            ),
            suite("SPEC")(
              test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
              test("Should call CompanyRepository.exists once"){assert(cnt.filter(_._1 == "exists"))(hasSize(equalTo(1)))},
              test("Should call CompanyRepository.exists with exact id"){assert(cnt.find(_._1 == "exists").map(_._2).flatten)(isSome(equalTo(uuid_ok)))},
              test("Should return exact value - CompanyRepository.exists"){assert(cnt.find(_._1 == "exists").map(_._3).flatten)(isSome(isRight(equalTo(true))))},
              test("Should NOT call CompanyRepository.insert"){assert(cnt.filter(_._1 == "insert"))(hasSize(equalTo(0)))}
            )
          )
        ),

        suite("Entity not inserted")(
          for {
            result <- ZIO.service[CompanyService].flatMap(_.add(company_new_err)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get).debug("Counter")
          } yield zio.Chunk(
            suite("TEST")(
              test("Should fail with exact error"){assert(result)(fails(equalTo(BadRequest(s"Bad request $uuid_new_err"))))}
            ),
            suite("SPEC")(
            test("Should call CompanyRepository.exists once"){assert(cnt.filter(_._1 == "exists"))(hasSize(equalTo(1)))},
            test("Should call CompanyRepository.exists with exact id"){assert(cnt.find(_._1 == "exists").map(_._2).flatten)(isSome(equalTo(uuid_new_err)))},
            test("Should return exact value - CompanyRepository.exists"){assert(cnt.find(_._1 == "exists").map(_._3).flatten)(isSome(isRight(equalTo(false))))},
            test("Should call CompanyRepository.insert once"){assert(cnt.filter(_._1 == "insert"))(hasSize(equalTo(1)))},
            test("Should call CompanyRepository.insert with exact value"){assert(cnt.find(_._1 == "insert").map(_._2).flatten)(isSome(equalTo(company_new_err)))},
            test("Should fail with exact error - CompanyRepository.insert"){assert(cnt.find(_._1 == "insert").map(_._3).flatten)(isSome(isLeft(equalTo(BadRequest(s"Bad request $uuid_new_err")))))},
            test("Should call CompanyRepository.exists before CompanyRepository.insert"){assert(cnt.map(_._1).indexOf("exists"))(isLessThan(cnt.map(_._1).indexOf("insert")))}
            )
          )
        )
      )
    )

    suite("delete")(test("..."){assertTrue(true)})
  }.provideLayer(CounterMock.layer ++ ((CompanyRepositoryMock.layer ++ DatabaseMock.layer) >>> CompanyServiceLive.live))
}
