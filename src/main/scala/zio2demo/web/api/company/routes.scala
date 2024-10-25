package zio2demo.web.api.company

import zio._
import zio.http._
import zio.uuid.UUIDGenerator
import zio.uuid.types.UUIDv7

object CompanyRoutes {
  import zio2demo.model.response.CompanyResponse
  import zio2demo.model.body.CompanyBody
  import zio2demo.model.response.{CreatedResponse, DeletedResponse}
  import zio2demo.model.ApplicationError._
  import zio2demo.controller.{CompanyController}
  import zio2demo.storage.driver.ConnectionPoolLive
  import zio2demo.storage.DatabaseLive
  import zio2demo.web.api.company.CompanyEndpoints

  private def handleGetCompanyEndPoint(uuid: java.util.UUID): ZIO[CompanyController, ApplicationError, CompanyResponse] =
    ZIO.logSpan("getCompany") {
      CompanyController.getCompany(UUIDv7.wrap(uuid))
        .map(CompanyResponse(_))
    }

  private def handleGetCompaniesEndPoint: ZIO[CompanyController, ApplicationError, Vector[CompanyResponse]] =
    ZIO.logSpan("getCompanies") {
      CompanyController.getCompanies
        .map(_.map(CompanyResponse(_)))
    }

  private def handlePostCompanyEndPoint(payload: CompanyBody): ZIO[CompanyController & UUIDGenerator, ApplicationError, CreatedResponse] =
    ZIO.logSpan("postCompanies") {
      CompanyController.addCompany(payload.name)
        .map((uuid: UUIDv7) => CreatedResponse(uuid.toString, "Company"))
    }

  private def handleDeleteCompanyEndPoint(uuid: java.util.UUID): ZIO[CompanyController, ApplicationError, DeletedResponse] =
    ZIO.logSpan("deleteCompanies") {
      CompanyController.deleteCompany(UUIDv7.wrap(uuid))
        .map((uuid: UUIDv7) => DeletedResponse(uuid.toString, "Company"))
    }

  def make: Routes[CompanyController & UUIDGenerator, Nothing] = Routes(
    CompanyEndpoints.get.implementHandler(handler(handleGetCompanyEndPoint)),
    // CompanyEndpoints.get.implement[CompanyController](handleGetCompanyEndPoint(_)),  // ^ same as above ^
    CompanyEndpoints.getAll.implementHandler(handler(handleGetCompaniesEndPoint)),
    // CompanyEndpoints.getAll.implement(_ => handleGetCompaniesEndPoint),                // ^ same as above ^
    CompanyEndpoints.insert.implementHandler(handler(handlePostCompanyEndPoint)),
    // CompanyEndpoints.insert.implement(handlePostCompanyEndPoint),                    // ^ same as above ^
    CompanyEndpoints.delete.implementHandler(handler(handleDeleteCompanyEndPoint))
    // CompanyEndpoints.delete.implement(handleDeleteCompanyEndPoint)                   // ^ same as above ^
  )
}
