package zio2demo.web.api.employee

import zio._
import zio.http._
import zio.uuid.types.UUIDv7
import zio.uuid.UUIDGenerator
import zio2demo.model.EntityType

object EmployeeRoutes {
  import zio2demo.model.body.EmployeeBody
  import zio2demo.model.response.{CreatedResponse, DeletedResponse, EmployeeResponse}
  import zio2demo.model.ApplicationError._
  import zio2demo.controller.{EmployeeController}
  import zio2demo.storage.driver.ConnectionPoolLive
  import zio2demo.storage.DatabaseLive

  private def handleGetEmployeeEndPoint(uuid: java.util.UUID): ZIO[EmployeeController, ApplicationError, EmployeeResponse] =
    ZIO.logSpan("getEmployee") {
      EmployeeController.getEmployee(UUIDv7.wrap(uuid))
        .map(EmployeeResponse(_))
    }

  private def handleGetEmployeesEndPoint: ZIO[EmployeeController, ApplicationError, Vector[EmployeeResponse]] =
    ZIO.logSpan("getEmployees") {
      EmployeeController.getEmployees
        .map(_.map(EmployeeResponse(_)))
    }

  private def handlePostEmployeeEndPoint(departmentId: java.util.UUID, payload: EmployeeBody): ZIO[EmployeeController & UUIDGenerator, ApplicationError, CreatedResponse] =
    ZIO.logSpan("postEmployee") {
      EmployeeController.addEmployee(payload.name, payload.email, payload.password, UUIDv7.wrap(departmentId))
        .map((uuid: UUIDv7) => CreatedResponse(uuid.toString, "Employee"))
    }

  private def handleDeleteEmployeeEndPoint(uuid: java.util.UUID): ZIO[EmployeeController, ApplicationError, DeletedResponse] =
    ZIO.logSpan("deleteEmployee") {
      EmployeeController.deleteEmployee(UUIDv7.wrap(uuid))
        .map((uuid: UUIDv7) => DeletedResponse(uuid.toString, "Employee"))
    }

  def make: Routes[EmployeeController & zio.uuid.UUIDGenerator, Nothing] = Routes(
    EmployeeEndpoints.get.implementHandler(handler(handleGetEmployeeEndPoint)),
    // EmployeeEndpoints.get.implement[EmployeeController](handleGetEmployeeEndPoint(_)),  // ^ same as above ^
    EmployeeEndpoints.getAll.implementHandler(handler(handleGetEmployeesEndPoint)),
    // EmployeeEndpoints.getAll.implement(_ => handleGetEmployeesEndPoint),                // ^ same as above ^
    EmployeeEndpoints.insert.implementHandler(handler(handlePostEmployeeEndPoint)),
    // EmployeeEndpoints.insert.implement{ case (departmentId: java.util.UUID, payload: EmployeeBody) => handlePostEmployeeEndPoint(departmentId, payload)},                    // ^ same as above ^
    EmployeeEndpoints.delete.implementHandler(handler(handleDeleteEmployeeEndPoint))
    // EmployeeEndpoints.delete.implement(handleDeleteEmployeeEndPoint)                   // ^ same as above ^
  )
}
