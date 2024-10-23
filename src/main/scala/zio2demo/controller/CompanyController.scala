package zio2demo.controller

import zio.{ZIO, URLayer, ZLayer, UIO, URIO}

import scala.util.chaining._

import zio2demo.service.CompanyService
import zio2demo.model.Company
import zio2demo.model.error.{ServiceError, EntityExistsError}
import zio2demo.model.error.{DatabaseError, ConnectionNotAvailable}

// Define service
trait CompanyController {
  def addCompany(id: Int, name: String): UIO[String]
}

// front-facing API (Accessor Methods)
object CompanyController {
  def addCompany(id: Int, name: String): URIO[CompanyController, String] =
    ZIO.serviceWithZIO[CompanyController](_.addCompany(id, name))
}

//  Implement service
case class CompanyControllerLive(companyService: CompanyService) extends CompanyController {
  def addCompany(id: Int, name: String): UIO[String] = {
    Company(id, name)
      .pipe(companyService.add)
      .as("Company registered successfully")
      .catchAll{
        case dbErr: DatabaseError => dbErr match {
          case ConnectionNotAvailable => ZIO
            .logError("An Database error occurred: Connection not available")
            .as("InternalServerError: An error occurred")
        }
        case serviceErr: ServiceError => serviceErr match {
          case e: EntityExistsError =>
            ZIO
              .logError(s"License plate ${e.id} already exists")
              .as(s"BadRequest: License plate ${e.id} already exists")
        }
      }
  }
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