package zio2demo.controller

import zio.test.ZIOSpecDefault

import zio._
import zio.mock._
import zio.test._

import zio.uuid.UUIDGenerator
import zio.uuid.types.{UUIDv1, UUIDv6, UUIDv7}
import java.util.UUID
import zio.Exit.{Success, Failure}

object DepartmentControllerSpec extends ZIOSpecDefault {
  import zio2demo.service.DepartmentService
  import zio2demo.model.Department
  import zio2demo.model.{ApplicationError, ErrorOrigin}
  import zio2demo.model.ApplicationError.{ApplicationError, NotFound, InternalServerError, BadRequest}

  object MockDepartmentService extends Mock[DepartmentService] {
    object Get extends Effect[UUIDv7, ApplicationError, Department]
    object GetAll extends Effect[Unit, ApplicationError, Vector[Department]]
    object Add extends Effect[Department, ApplicationError, Unit]
    object Delete extends Effect[UUIDv7, ApplicationError, Unit]

    override val compose: URLayer[Proxy, DepartmentService] = ZLayer {
      ZIO.serviceWithZIO[Proxy] { (proxy: Proxy) =>
          ZIO.succeed{
            new DepartmentService {
              def get(uuid: UUIDv7): IO[ApplicationError, Department] = proxy(Get, uuid)
              def getAll: IO[ApplicationError, Vector[Department]] = proxy(GetAll)
              def add(department: Department): IO[ApplicationError, Unit] = proxy(Add, department)
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

  def spec = suite("Department Controller") {
    val uuid = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
    val uuid2 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
    val companyID = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"))

    val liveDepartmentController = DepartmentControllerLive.live

    zio.Chunk(
      suite("getDepartment"){
        zio.Chunk(
          test ("Happy path") {
            val expected: Department = Department(uuid, "Dev", companyID)
            val sut: ZIO[DepartmentController, ApplicationError, Department] = DepartmentController.getDepartment(uuid)
            val mockGet: ULayer[DepartmentService] = MockDepartmentService.Get(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.value[Department](expected)
            ).toLayer

            sut.provideLayer(mockGet >>> liveDepartmentController)
              .map((department: Department) => assertTrue(department == expected))
          },

          test ("Error path") {
            val expected: ApplicationError = NotFound(s"Department with id ${uuid} not found!")
            val sut: ZIO[DepartmentController, ApplicationError, Department] = DepartmentController.getDepartment(uuid)
            val mockGet: ULayer[DepartmentService] = MockDepartmentService.Get(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.failure[ApplicationError](expected)
            ).toLayer

            for {
              result <- sut.provideLayer(mockGet >>> liveDepartmentController).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expected))
            )
          }
        )
      },

      suite("getDepartments"){
        zio.Chunk(
          test ("Happy path") {
            val expected: Vector[Department] = Vector(Department(uuid, "Dev", companyID), Department(uuid2, "Desk", companyID))
            val sut: ZIO[DepartmentController, ApplicationError, Vector[Department]] = DepartmentController.getDepartments
            val mockGetAll: ULayer[DepartmentService] = MockDepartmentService.GetAll(
              Expectation.value(expected)
            ).toLayer

            sut.provideLayer(mockGetAll >>> liveDepartmentController)
              .map((departments: Vector[Department]) => assertTrue(departments == expected))
          },

          test ("Error path") {
            val expectedErrror: ApplicationError = InternalServerError("Some error!", ErrorOrigin.DatabaseError())
            val sut: ZIO[DepartmentController, ApplicationError, Vector[Department]] = DepartmentController.getDepartments
            val mockGetAll: ULayer[DepartmentService] = MockDepartmentService.GetAll(
              Expectation.failure[ApplicationError](expectedErrror)
            ).toLayer

            for {
              result <- sut.provideLayer(mockGetAll >>> liveDepartmentController).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expectedErrror))
            )
          }
        )
      },

      suite("addDepartment"){
        zio.Chunk(
          test ("Happy path") {
            val expected: UUIDv7 = uuid
            val sut: ZIO[DepartmentController & UUIDGenerator, ApplicationError, UUIDv7] = DepartmentController.addDepartment("Sales", companyID)
            val mockAdd: ULayer[DepartmentService] = MockDepartmentService.Add(
              assertion = Assertion.equalTo[Department](Department(uuid, "Sales", companyID)),
              result = Expectation.value(())
            ).toLayer
            val mockUUIDv7: ULayer[UUIDGenerator] = MockUUIDGenerator.UUIDV7(
              Expectation.value(uuid)
            ).toLayer

            sut.provideLayer((mockAdd >>> liveDepartmentController) ++ mockUUIDv7)
              .map((id: UUIDv7) => assertTrue(id == expected))
          },

          test ("Error path") {
            val expectedErrror: ApplicationError = BadRequest(s"Department with id ${uuid} already exists!")
            val sut: ZIO[DepartmentController & UUIDGenerator, ApplicationError, UUIDv7] = DepartmentController.addDepartment("Sales", companyID)
            val mockAdd: ULayer[DepartmentService] = MockDepartmentService.Add(
              assertion = Assertion.equalTo[Department](Department(uuid, "Sales", companyID)),
              Expectation.failure[ApplicationError](expectedErrror)
            ).toLayer
            val mockUUIDv7: ULayer[UUIDGenerator] = MockUUIDGenerator.UUIDV7(
              Expectation.value(uuid)
            ).toLayer

            for {
              result <- sut.provideLayer((mockAdd >>> liveDepartmentController) ++ mockUUIDv7).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expectedErrror))
            )
          }
        )
      },

      suite("deleteDepartment"){
        zio.Chunk(
          test ("Happy path") {
            val sut: ZIO[DepartmentController, ApplicationError, UUIDv7] = DepartmentController.deleteDepartment(uuid)
            val mockDelete: ULayer[DepartmentService] = MockDepartmentService.Delete(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.value[Unit](())
            ).toLayer

            sut.provideLayer(mockDelete >>> liveDepartmentController)
              .map((id: UUIDv7) => assertTrue(id == uuid))
          },

          test ("Error path") {
            val expected: ApplicationError = NotFound(s"Department with id ${uuid} does not exists!")
            val sut: ZIO[DepartmentController, ApplicationError, UUIDv7] = DepartmentController.deleteDepartment(uuid)
            val mockDelete: ULayer[DepartmentService] = MockDepartmentService.Delete(
              assertion = Assertion.equalTo(uuid),
              result = Expectation.failure[ApplicationError](expected)
            ).toLayer

            for {
              result <- sut.provideLayer(mockDelete >>> liveDepartmentController).exit
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