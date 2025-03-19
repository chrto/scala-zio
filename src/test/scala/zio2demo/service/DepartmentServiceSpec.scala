package zio2demo.service

import zio.test.ZIOSpecDefault
import zio.{ZIO, ZLayer, ULayer, Ref, IO, Tag, Scope}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.all._
import zio.Console.ConsoleLive
import zio.mock.MockConsole
import scala.reflect.ClassTag

object DepartmentServiceSpec extends ZIOSpecDefault {
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

  object CounterMock {
    val layer: ZLayer[Any, Nothing, Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]] =
      ZLayer.fromZIO(Ref.make(Vector.empty[(String, Option[Any], Option[Either[ApplicationError, Any]])]))
  }

  class DepartmentRepositoryMock(departmentRepositoryLive: DepartmentRepository, ref: Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]) extends DepartmentRepository {
    private def updateRef[I, O](name: String, in: I, result: ZIO[Connection, ApplicationError, O]): ZIO[Connection, ApplicationError, O] =
      result.tapBoth(
        (failure: ApplicationError) => ref.update(_ :+ (name, in.pure[Option], failure.asLeft[O].pure[Option])),
        (value: O) => ref.update(_ :+ (name, in.pure[Option], value.asRight[ApplicationError].pure[Option]))
      )

    def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit] =
      this.updateRef[UUIDv7, Unit]("delete", uuid, departmentRepositoryLive.delete(uuid))

    def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean] =
      this.updateRef[UUIDv7, Boolean]("exists", uuid, departmentRepositoryLive.exists(uuid))

    def getUnsafe(uuid: UUIDv7): ZIO[Connection, ApplicationError, Department] =
      this.updateRef[UUIDv7, Department]("getUnsafe", uuid, departmentRepositoryLive.getUnsafe(uuid))

    def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Department] =
      this.updateRef[UUIDv7, Department]("get", uuid, departmentRepositoryLive.get(uuid))

    def getAllUnsafe: ZIO[Connection, ApplicationError, Seq[Department]] =
      this.updateRef[Unit, Seq[Department]]("getAllUnsafe", (), departmentRepositoryLive.getAllUnsafe)

    def getAll: ZIO[Connection, ApplicationError, Seq[Department]] =
      this.updateRef[Unit, Seq[Department]]("getAll", (), departmentRepositoryLive.getAll)

    def insert(department: Department): ZIO[Connection, ApplicationError, Unit] =
      this.updateRef[Department, Unit]("insert", department, departmentRepositoryLive.insert(department))
  }

  object DepartmentRepositoryMock {
    val layer: ULayer[DepartmentRepository] = CounterMock.layer >>> ZLayer.fromFunction(DepartmentRepositoryMock(DepartmentRepositoryLive() ,_))
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
        case id if id.compareTo(uuid_ok) == 0 => ZIO.succeed[Option[E]](department_ok.asInstanceOf[E].pure[Option])
        case id if id.compareTo(uuid_new_err) == 0 => ZIO.succeed[Option[E]](None)
        case id if id.compareTo(uuid_new) == 0 => ZIO.succeed[Option[E]](None)
        case id => ZIO.fail(BadRequest(s"Bad request $id"))

    def getAllUnsafe[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Seq[E]] = ???

    def getAll[E <: Entity: ClassTag](using entity: EntityType[E]): IO[ApplicationError, Vector[E]] = ???
    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] = ???
  }

  object ConnectionMock {
    val id: String = "connectionMockId"
    val layer: ULayer[ConnectionMock] = ZLayer.succeed(ConnectionMock())
  }

  class DatabaseMock(ref: Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]) extends Database {
    def transact[R, A: Tag](dbProgram: ZIO[Connection & R, ApplicationError, A]): ZIO[R, ApplicationError, A] =
      ref.update(_ :+ ("transact", None, None)) *> dbProgram.provideSomeLayer[R](ConnectionMock.layer)
  }

  object DatabaseMock {
    val layer: ULayer[Database] = CounterMock.layer >>> ZLayer.fromFunction(DatabaseMock(_))
  }

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suiteAll("Department Service - SPEC"){
    suite("get")(
      suite("Happy path") {
        for {
          result <- ZIO.service[DepartmentService].flatMap(_.get(uuid_ok))
          cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
        } yield zio.Chunk(
          test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
          test("Should call DepartmentRepository.get once"){assert(cnt.filter(_._1 == "get"))(hasSize(equalTo(1)))},
          test("Should call DepartmentRepository.get with exact id"){assert(cnt.find(_._1 == "get").map(_._2).flatten)(isSome(equalTo(uuid_ok)))},
          test("Should return exact value - DepartmentRepository.get"){assert(cnt.find(_._1 == "get").map(_._3).flatten)(isSome(isRight(equalTo(department_ok))))}
        )
      },
      suite("Error path") {zio.Chunk(
        suite("Not Found")(
          for {
            result <- ZIO.service[DepartmentService].flatMap(_.get(uuid_new)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
          } yield  zio.Chunk(
            test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.get once"){assert(cnt.filter(_._1 == "get"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.get with exact id"){assert(cnt.find(_._1 == "get").map(_._2).flatten)(isSome(equalTo(uuid_new)))},
            test("Should fail with exact error - DepartmentRepository.get"){assert(cnt.find(_._1 == "get").map(_._3).flatten)(isSome(isLeft(equalTo(NotFound(s"Department with id ${uuid_new} not found!")))))}
          )
        ),
        suite("Failed"){
          for {
            result <- ZIO.service[DepartmentService].flatMap(_.get(uuid_err)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
          } yield zio.Chunk(
            test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.get once"){assert(cnt.filter(_._1 == "get"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.get with exact id"){assert(cnt.find(_._1 == "get").map(_._2).flatten)(isSome(equalTo(uuid_err)))},
            test("Should fail with exact error - DepartmentRepository.get"){assert(cnt.find(_._1 == "get").map(_._3).flatten)(isSome(isLeft(equalTo(BadRequest(s"Bad request $uuid_err")))))}
          )
        }
      )}
    )

    suite("getAll")(test("..."){assertTrue(true)})

    suite("add")(
      suite("Happy path")(
        for {
          result <- ZIO.service[DepartmentService].flatMap(_.add(department_new))
          cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
        } yield zio.Chunk(
          test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
          test("Should call DepartmentRepository.exists once"){assert(cnt.filter(_._1 == "exists"))(hasSize(equalTo(1)))},
          test("Should call DepartmentRepository.exists with exact id"){assert(cnt.find(_._1 == "exists").map(_._2).flatten)(isSome(equalTo(uuid_new)))},
          test("Should return exact value - DepartmentRepository.exists"){assert(cnt.find(_._1 == "exists").map(_._3).flatten)(isSome(isRight(equalTo(false))))},
          test("Should call DepartmentRepository.insert once"){assert(cnt.filter(_._1 == "insert"))(hasSize(equalTo(1)))},
          test("Should call DepartmentRepository.insert with exact value"){assert(cnt.find(_._1 == "insert").map(_._2).flatten)(isSome(equalTo(department_new)))},
          test("Should return exact value - DepartmentRepository.insert"){assert(cnt.find(_._1 == "insert").map(_._3).flatten)(isSome(isRight(equalTo(()))))},
          test("Should call DepartmentRepository.exists before DepartmentRepository.insert"){assert(cnt.map(_._1).indexOf("exists"))(isLessThan(cnt.map(_._1).indexOf("insert")))}
        )
      ),

      suite("Error path")(
        suite("Entity exists")(
          for {
            result <- ZIO.service[DepartmentService].flatMap(_.add(department_ok)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
          } yield zio.Chunk(
            test("Should open transaction"){assert(cnt.filter(_._1 == "transact"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.exists once"){assert(cnt.filter(_._1 == "exists"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.exists with exact id"){assert(cnt.find(_._1 == "exists").map(_._2).flatten)(isSome(equalTo(uuid_ok)))},
            test("Should return exact value - DepartmentRepository.exists"){assert(cnt.find(_._1 == "exists").map(_._3).flatten)(isSome(isRight(equalTo(true))))},
            test("Should NOT call DepartmentRepository.insert"){assert(cnt.filter(_._1 == "insert"))(hasSize(equalTo(0)))}
          )
        ),

        suite("Entity not inserted")(
          for {
            result <- ZIO.service[DepartmentService].flatMap(_.add(department_new_err)).exit
            cnt <- ZIO.service[Ref[Vector[(String, Option[Any], Option[Either[ApplicationError, Any]])]]].flatMap(_.get)
          } yield zio.Chunk(
            test("Should call DepartmentRepository.exists once"){assert(cnt.filter(_._1 == "exists"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.exists with exact id"){assert(cnt.find(_._1 == "exists").map(_._2).flatten)(isSome(equalTo(uuid_new_err)))},
            test("Should return exact value - DepartmentRepository.exists"){assert(cnt.find(_._1 == "exists").map(_._3).flatten)(isSome(isRight(equalTo(false))))},
            test("Should call DepartmentRepository.insert once"){assert(cnt.filter(_._1 == "insert"))(hasSize(equalTo(1)))},
            test("Should call DepartmentRepository.insert with exact value"){assert(cnt.find(_._1 == "insert").map(_._2).flatten)(isSome(equalTo(department_new_err)))},
            test("Should fail with exact error - DepartmentRepository.insert"){assert(cnt.find(_._1 == "insert").map(_._3).flatten)(isSome(isLeft(equalTo(BadRequest(s"Bad request $uuid_new_err")))))},
            test("Should call DepartmentRepository.exists before DepartmentRepository.insert"){assert(cnt.map(_._1).indexOf("exists"))(isLessThan(cnt.map(_._1).indexOf("insert")))}
          )
        )
      )
    )

    suite("delete")(test("..."){assertTrue(true)})
  }.provideLayer(CounterMock.layer ++ ((DepartmentRepositoryMock.layer ++ DatabaseMock.layer) >>> DepartmentServiceLive.live)) @@ TestAspect.silentLogging
}
