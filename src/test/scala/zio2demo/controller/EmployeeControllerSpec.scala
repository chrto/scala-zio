package zio2demo.controller

import zio.test.ZIOSpecDefault

import zio._
import zio.mock._
import zio.test._

import zio.uuid.UUIDGenerator
import zio.uuid.types.{UUIDv1, UUIDv6, UUIDv7}
import java.util.UUID
import zio.Exit.{Success, Failure}

object EmployeeControllerSpec extends ZIOSpecDefault {
  import zio2demo.service.EmployeeService
  import zio2demo.model.Employee
  import zio2demo.model.{ApplicationError, ErrorOrigin}
  import zio2demo.model.ApplicationError.{ApplicationError, NotFound, InternalServerError, BadRequest}

  object MockEmployeeService extends Mock[EmployeeService] {
    object Get extends Effect[UUIDv7, ApplicationError, Employee]
    object GetByEmail extends Effect[String, ApplicationError, Employee]
    object GetByCredentials extends Effect[(String, String), ApplicationError, Employee]
    object GetAll extends Effect[Unit, ApplicationError, Vector[Employee]]
    object Add extends Effect[Employee, ApplicationError, Unit]
    object Delete extends Effect[UUIDv7, ApplicationError, Unit]

    override val compose: URLayer[Proxy, EmployeeService] = ZLayer {
      ZIO.serviceWithZIO[Proxy] { (proxy: Proxy) =>
          ZIO.succeed{
            new EmployeeService {
              def get(uuid: UUIDv7): IO[ApplicationError, Employee] = proxy(Get, uuid)
              def getByEmail(email: String): IO[ApplicationError, Employee] = proxy(GetByEmail, email)
              def getByCredentials(userName: String, pwdHash: String): IO[ApplicationError, Employee] = proxy(GetByCredentials, (userName, pwdHash))
              def getAll: IO[ApplicationError, Vector[Employee]] = proxy(GetAll)
              def add(employee: Employee): IO[ApplicationError, Unit] = proxy(Add, employee)
              def delete(uuid: UUIDv7): IO[ApplicationError, Unit] = proxy(Delete, uuid)
            }
          }
      }
    }
  }

  object MockUUIDGenerator extends Mock[UUIDGenerator] {
    object UUIDV1 extends Effect[Unit, Nothing, UUIDv1]
    object UUIDV6 extends Effect[Unit, Nothing, UUIDv6]
    object UUIDV7 extends Effect[Unit, Nothing, UUIDv7]

    override val compose: URLayer[Proxy, UUIDGenerator] = ZLayer {
      ZIO.serviceWithZIO[Proxy] { (proxy: Proxy) =>
          ZIO.succeed{
            new UUIDGenerator {
              def uuidV1: IO[Nothing, UUIDv1] = proxy(UUIDV1)
              def uuidV6: IO[Nothing, UUIDv6] = proxy(UUIDV6)
              def uuidV7: IO[Nothing, UUIDv7] = proxy(UUIDV7)
            }
          }
      }
    }
  }

  def spec = suite("Employee Controller") {
    val uuid = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
    val uuid2 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
    val departmentID = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"))
    val liveEmployeeController = EmployeeControllerLive.live
    zio.Chunk(
      suite("getEmployee"){
        zio.Chunk(
          test ("Happy path") {
            val expected: Employee = Employee(uuid, "Joe Doe", "joe.doe@tescos.com", "pwdhash", departmentID)
            val sut: ZIO[EmployeeController, ApplicationError, Employee] = EmployeeController.getEmployee(uuid)
            val mockGet: ULayer[EmployeeService] = MockEmployeeService.Get(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.value[Employee](expected)
            ).toLayer

            sut.provideLayer(mockGet >>> liveEmployeeController)
              .map((employee: Employee) => assertTrue(employee == expected))
          },


          test ("Error path") {
            val expected: ApplicationError = NotFound(s"Employee with id ${uuid} not found!")
            val sut: ZIO[EmployeeController, ApplicationError, Employee] = EmployeeController.getEmployee(uuid)
            val mockGet: ULayer[EmployeeService] = MockEmployeeService.Get(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.failure[ApplicationError](expected)
            ).toLayer

            for {
              result <- sut.provideLayer(mockGet >>> liveEmployeeController).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expected))
            )
          }
        )
      },

      suite("getEmployees"){
        zio.Chunk(
          test ("Happy path") {
            val expected: Vector[Employee] = Vector(Employee(uuid, "Joe Doe", "joe.doe@tescos.com", "joePwdhash", departmentID), Employee(uuid2, "Jack Black", "jack.black@tescos.com", "jackPwdhash", departmentID))
            val sut: ZIO[EmployeeController, ApplicationError, Vector[Employee]] = EmployeeController.getEmployees
            val mockGetAll: ULayer[EmployeeService] = MockEmployeeService.GetAll(
              Expectation.value(expected)
            ).toLayer

            sut.provideLayer(mockGetAll >>> liveEmployeeController)
              .map((employees: Vector[Employee]) => assertTrue(employees == expected))
          },

          test ("Error path") {
            val expectedErrror: ApplicationError = InternalServerError("Some error!", ErrorOrigin.DatabaseError())
            val sut: ZIO[EmployeeController, ApplicationError, Vector[Employee]] = EmployeeController.getEmployees
            val mockGetAll: ULayer[EmployeeService] = MockEmployeeService.GetAll(
              Expectation.failure[ApplicationError](expectedErrror)
            ).toLayer

            for {
              result <- sut.provideLayer(mockGetAll >>> liveEmployeeController).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expectedErrror))
            )
          }
        )
      },

      suite("addEmployee"){
        zio.Chunk(
          test ("Happy path") {
            val expected: UUIDv7 = uuid
            val sut: ZIO[EmployeeController & UUIDGenerator, ApplicationError, UUIDv7] = EmployeeController.addEmployee("Joe Doe", "joe.doe@tescos.com", "joePwdhash", departmentID)
            val mockAdd: ULayer[EmployeeService] = MockEmployeeService.Add(
              assertion = Assertion.equalTo[Employee](Employee(uuid, "Joe Doe", "joe.doe@tescos.com", "joePwdhash", departmentID)),
              result = Expectation.value(())
            ).toLayer
            val mockUUIDv7: ULayer[UUIDGenerator] = MockUUIDGenerator.UUIDV7(
              Expectation.value(uuid)
            ).toLayer

            sut.provideLayer((mockAdd >>> liveEmployeeController) ++ mockUUIDv7)
              .map((id: UUIDv7) => assertTrue(id == expected))
          },

          test ("Error path") {
            val expectedErrror: ApplicationError = BadRequest(s"Employee with id ${uuid} already exists!")
            val sut: ZIO[EmployeeController & UUIDGenerator, ApplicationError, UUIDv7] = EmployeeController.addEmployee("Joe Doe", "joe.doe@tescos.com", "joePwdhash", departmentID)
            val mockAdd: ULayer[EmployeeService] = MockEmployeeService.Add(
              assertion = Assertion.equalTo[Employee](Employee(uuid, "Joe Doe", "joe.doe@tescos.com", "joePwdhash", departmentID)),
              Expectation.failure[ApplicationError](expectedErrror)
            ).toLayer
            val mockUUIDv7: ULayer[UUIDGenerator] = MockUUIDGenerator.UUIDV7(
              Expectation.value(uuid)
            ).toLayer

            for {
              result <- sut.provideLayer((mockAdd >>> liveEmployeeController) ++ mockUUIDv7).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expectedErrror))
            )
          }
        )
      },

      suite("deleteEmployee"){
        zio.Chunk(
          test ("Happy path") {
            val sut: ZIO[EmployeeController, ApplicationError, UUIDv7] = EmployeeController.deleteEmployee(uuid)
            val mockDelete: ULayer[EmployeeService] = MockEmployeeService.Delete(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.value[Unit](())
            ).toLayer

            sut.provideLayer(mockDelete >>> liveEmployeeController)
              .map((id: UUIDv7) => assertTrue(id == uuid))
          },

          test ("Error path") {
            val expected: ApplicationError = NotFound(s"Employee with id ${uuid} does not exists!")
            val sut: ZIO[EmployeeController, ApplicationError, UUIDv7] = EmployeeController.deleteEmployee(uuid)
            val mockDelete: ULayer[EmployeeService] = MockEmployeeService.Delete(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.failure[ApplicationError](expected)
            ).toLayer

            for {
              result <- sut.provideLayer(mockDelete >>> liveEmployeeController).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expected))
            )
          }
        )
      }
    )
  }
}