package zio2demo.controller

import zio.test.ZIOSpecDefault

import zio._
import zio.mock._
import zio.test._

import zio.uuid.UUIDGenerator
import zio.uuid.types.{UUIDv1, UUIDv6, UUIDv7}
import java.util.UUID
import zio.Exit.{Success, Failure}

object CompanyControllerSpec extends ZIOSpecDefault {
  import zio2demo.service.CompanyService
  import zio2demo.model.Company
  import zio2demo.model.{ApplicationError, ErrorOrigin}
  import zio2demo.model.ApplicationError.{ApplicationError, NotFound, InternalServerError, BadRequest}

  object MockCompanyService extends Mock[CompanyService] {
    object Get extends Effect[UUIDv7, ApplicationError, Company]
    object GetAll extends Effect[Unit, ApplicationError, Vector[Company]]
    object Add extends Effect[Company, ApplicationError, Unit]
    object Delete extends Effect[UUIDv7, ApplicationError, Unit]

    override val compose: URLayer[Proxy, CompanyService] = ZLayer {
      ZIO.serviceWithZIO[Proxy] { (proxy: Proxy) =>
        // withRuntime[Proxy, CompanyService] { (runtime: Runtime[Proxy]) => // not needed here
          ZIO.succeed{
            new CompanyService {
              def get(uuid: UUIDv7): IO[ApplicationError, Company] = proxy(Get, uuid)
              def getAll: IO[ApplicationError, Vector[Company]] = proxy(GetAll)
              def add(company: Company): IO[ApplicationError, Unit] = proxy(Add, company)
              def delete(uuid: UUIDv7): IO[ApplicationError, Unit] = proxy(Delete, uuid)
            }
          }
        // }
      }
    }

    // override val compose: URLayer[Proxy, CompanyService] = ZLayer {
    //   for {
    //     proxy <- ZIO.service[Proxy]
    //   } yield new CompanyService {
    //     def get(uuid: UUIDv7): IO[ApplicationError, Company] = proxy(Get, uuid)
    //     def getAll: IO[ApplicationError, Vector[Company]] = proxy(GetAll, ())
    //     def add(company: Company): IO[ApplicationError, Unit] = proxy(Add, company)
    //     def delete(uuid: UUIDv7): IO[ApplicationError, Unit] = proxy(Delete, uuid)
    //   }
    // }
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

  def spec = suite("Company Controller") {
    val uuid = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
    val uuid2 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
    val liveCompanyController = CompanyControllerLive.live
    zio.Chunk(
      suite("getCompany"){
        zio.Chunk(
          test ("Happy path") {
            val sut: ZIO[CompanyController, ApplicationError, Company] = CompanyController.getCompany(uuid)
            val mockGet: ULayer[CompanyService] = MockCompanyService.Get(
              assertion = Assertion.isOneOf(Vector(uuid, uuid2)),
              // result = Expectation.value(Company(uuid, "Tescos"))
              result = Expectation.valueF[UUIDv7, Company]((id: UUIDv7)  => Company(id, "Tescos"))
            ).toLayer

            sut.provideLayer(mockGet >>> liveCompanyController)
              .map((company: Company) => assertTrue(company == Company(uuid, "Tescos")))
          },

          test ("Error path") {
            val sut: ZIO[CompanyController, ApplicationError, Company] = CompanyController.getCompany(uuid2)
            val mockGet: ULayer[CompanyService] = MockCompanyService.Get(
              assertion = Assertion.isOneOf(Vector(uuid, uuid2)),
              // result = Expectation.failure(NotFound(s"Company with id ${uuid2} not found!"))
              result = Expectation.failureF[UUIDv7, ApplicationError]((id: UUIDv7) => NotFound(s"Company with id ${id} not found!"))
            ).toLayer

            for {
              result <- sut.provideLayer(mockGet >>> liveCompanyController).exit
              // result <- sut.provide(testLayer).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(NotFound(s"Company with id ${uuid2} not found!")))
            )
          }
        )
      },

      suite("getCompanies"){
        zio.Chunk(
          test ("Happy path") {
            val expected: Vector[Company] = Vector(Company(uuid, "Tescos"), Company(uuid2, "Sainsburys"))
            val sut: ZIO[CompanyController, ApplicationError, Vector[Company]] = CompanyController.getCompanies
            val mockGetAll: ULayer[CompanyService] = MockCompanyService.GetAll(
              Expectation.value(expected)
            ).toLayer

            sut.provideLayer(mockGetAll >>> liveCompanyController)
              .map((companies: Vector[Company]) => assertTrue(companies == expected))
          },

          test ("Error path") {
            val expectedErrror: ApplicationError = InternalServerError("Some error!", ErrorOrigin.DatabaseError())
            val sut: ZIO[CompanyController, ApplicationError, Vector[Company]] = CompanyController.getCompanies
            val mockGetAll: ULayer[CompanyService] = MockCompanyService.GetAll(
              Expectation.failure[ApplicationError](expectedErrror)
            ).toLayer

            for {
              result <- sut.provideLayer(mockGetAll >>> liveCompanyController).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expectedErrror))
            )
          }
        )
      },

      suite("addCompany"){
        zio.Chunk(
          test ("Happy path") {
            val expected: UUIDv7 = uuid
            val sut: ZIO[CompanyController & UUIDGenerator, ApplicationError, UUIDv7] = CompanyController.addCompany("Tescos")
            val mockAdd: ULayer[CompanyService] = MockCompanyService.Add(
              assertion = Assertion.equalTo[Company](Company(uuid, "Tescos")),
              result = Expectation.value(())
            ).toLayer
            val mockUUIDv7: ULayer[UUIDGenerator] = MockUUIDGenerator.UUIDV7(
              Expectation.value(uuid)
            ).toLayer

            sut.provideLayer((mockAdd >>> liveCompanyController) ++ mockUUIDv7)
              .map((id: UUIDv7) => assertTrue(id == expected))
          },

          test ("Error path") {
            val expectedErrror: ApplicationError = BadRequest(s"Company with id ${uuid} already exists!")
            val sut: ZIO[CompanyController & UUIDGenerator, ApplicationError, UUIDv7] = CompanyController.addCompany("Tescos")
            val mockAdd: ULayer[CompanyService] = MockCompanyService.Add(
              assertion = Assertion.equalTo[Company](Company(uuid, "Tescos")),
              Expectation.failure[ApplicationError](expectedErrror)
            ).toLayer
            val mockUUIDv7: ULayer[UUIDGenerator] = MockUUIDGenerator.UUIDV7(
              Expectation.value(uuid)
            ).toLayer

            for {
              result <- sut.provideLayer((mockAdd >>> liveCompanyController) ++ mockUUIDv7).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(expectedErrror))
            )
          }
        )
      },

      suite("deleteCompany"){
        zio.Chunk(
          test ("Happy path") {
            val sut: ZIO[CompanyController, ApplicationError, UUIDv7] = CompanyController.deleteCompany(uuid)
            val mockDelete: ULayer[CompanyService] = MockCompanyService.Delete(
              assertion = Assertion.isOneOf(Vector(uuid, uuid2)),
              result = Expectation.value[Unit](())
            ).toLayer

            sut.provideLayer(mockDelete >>> liveCompanyController)
              .map((id: UUIDv7) => assertTrue(id == uuid))
          },

          test ("Error path") {
            val sut: ZIO[CompanyController, ApplicationError, UUIDv7] = CompanyController.deleteCompany(uuid)
            val mockDelete: ULayer[CompanyService] = MockCompanyService.Delete(
              assertion = Assertion.isOneOf(Vector(uuid, uuid2)),
              result = Expectation.failureF[UUIDv7, ApplicationError]((id: UUIDv7) => NotFound(s"Company with id ${uuid} does not exists!"))
            ).toLayer

            for {
              result <- sut.provideLayer(mockDelete >>> liveCompanyController).exit
            } yield assertTrue(result match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(NotFound(s"Company with id ${uuid} does not exists!")))
            )
          }
        )
      }
    )
  }
}