package zio2demo.controller

import zio.{ZIO, URLayer, ZLayer, IO}
import zio.uuid.UUIDGenerator
import zio.uuid.types.UUIDv7
import zio2demo.service.CompanyService
import zio2demo.model.Company
import zio2demo.model.ApplicationError._

// Define service
trait CompanyController {
  def getCompany(uuid: UUIDv7): IO[ApplicationError, Company]
  def getCompanies: IO[ApplicationError, Vector[Company]]
  def addCompany(name: String): ZIO[UUIDGenerator, ApplicationError, UUIDv7]
  def deleteCompany(uuid: UUIDv7): IO[ApplicationError, UUIDv7]
}

// front-facing API (Accessor Methods)
object CompanyController {
  def getCompany(uuid: UUIDv7): ZIO[CompanyController, ApplicationError, Company] =
    ZIO.serviceWithZIO[CompanyController](_.getCompany(uuid))

  def getCompanies: ZIO[CompanyController, ApplicationError, Vector[Company]] =
    ZIO.serviceWithZIO[CompanyController](_.getCompanies)

  def addCompany(name: String): ZIO[CompanyController & UUIDGenerator, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[CompanyController](_.addCompany(name))

  def deleteCompany(uuid: UUIDv7): ZIO[CompanyController, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[CompanyController](_.deleteCompany(uuid))
}

//  Implement service
case class CompanyControllerLive(companyService: CompanyService) extends CompanyController {
  def getCompany(uuid: UUIDv7): IO[ApplicationError, Company] =
    companyService.get(uuid)

  def getCompanies: IO[ApplicationError, Vector[Company]] =
    companyService.getAll.map(_.toVector)

  def addCompany(name: String): ZIO[UUIDGenerator, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[UUIDGenerator](_.uuidV7)
      .flatMap((uuid: UUIDv7) => companyService.add(Company(uuid, name)).as[UUIDv7](uuid))

  def deleteCompany(uuid: UUIDv7): IO[ApplicationError, UUIDv7] =
    companyService.delete(uuid)
      .as[UUIDv7](uuid)
}

// lift the service implementation into the ZLayer
object CompanyControllerLive {
  val live: URLayer[CompanyService, CompanyController] = ZLayer.fromFunction(CompanyControllerLive(_))
  // same as:
  val live2: URLayer[CompanyService, CompanyController] =
    ZLayer {
      for {
        companyService <- ZIO.service[CompanyService]
      } yield CompanyControllerLive(companyService)
    }
  // same as:
  val live3: URLayer[CompanyService, CompanyController]  =
    ZLayer(ZIO.service[CompanyService].map(CompanyControllerLive(_)))
}