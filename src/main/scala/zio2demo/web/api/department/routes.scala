package zio2demo.web.api.department

import zio._
import zio.http._
import zio.uuid.types.UUIDv7
import zio.uuid.UUIDGenerator

object DepartmentRoutes {
  import zio2demo.model.body.DepartmentBody
  import zio2demo.model.response.{CreatedResponse, DeletedResponse, DepartmentResponse}
  import zio2demo.model.ApplicationError._
  import zio2demo.controller.DepartmentController

  private def handleGetDepartmentEndPoint(uuid: java.util.UUID): ZIO[DepartmentController, ApplicationError, DepartmentResponse] =
    ZIO.logSpan("getDepartment") {
      DepartmentController.getDepartment(UUIDv7.wrap(uuid))
        .map(DepartmentResponse(_))
    }

  private def handleGetDepartmentsEndPoint: ZIO[DepartmentController, ApplicationError, Vector[DepartmentResponse]] =
    ZIO.logSpan("getDepartments") {
      DepartmentController.getDepartments
        .map(_.map(DepartmentResponse(_)))
    }

  private def handlePostDepartmentEndPoint(companyId: java.util.UUID, payload: DepartmentBody): ZIO[DepartmentController & UUIDGenerator, ApplicationError, CreatedResponse] =
    ZIO.logSpan("postDepartment") {
      DepartmentController.addDepartment(payload.name, UUIDv7.wrap(companyId))
        .map((uuid: UUIDv7) => CreatedResponse(uuid.toString, "Department"))
    }

  private def handleDeleteDepartmentEndPoint(uuid: java.util.UUID): ZIO[DepartmentController, ApplicationError, DeletedResponse] =
    ZIO.logSpan("deleteDepartment") {
      DepartmentController.deleteDepartment(UUIDv7.wrap(uuid))
        .map((uuid: UUIDv7) => DeletedResponse(uuid.toString, "Department"))
    }

  def make: Routes[DepartmentController & UUIDGenerator, Nothing] = Routes(
    DepartmentEndpoints.get.implementHandler(handler(handleGetDepartmentEndPoint)),
    // DepartmentEndpoints.get.implement[DepartmentController](handleGetDepartmentEndPoint(_)),  // ^ same as above ^
    DepartmentEndpoints.getAll.implementHandler(handler(handleGetDepartmentsEndPoint)),
    // DepartmentEndpoints.getAll.implement(_ => handleGetDepartmentsEndPoint),                // ^ same as above ^
    DepartmentEndpoints.insert.implementHandler(handler(handlePostDepartmentEndPoint)),
    // DepartmentEndpoints.insert.implement{ case (companyId: java.util.UUID, payload: DepartmentBody) => handlePostDepartmentEndPoint(companyId, payload)},                    // ^ same as above ^
    DepartmentEndpoints.delete.implementHandler(handler(handleDeleteDepartmentEndPoint))
    // // DepartmentEndpoints.delete.implement(handleDeleteDepartmentEndPoint)                   // ^ same as above ^
  )
}
