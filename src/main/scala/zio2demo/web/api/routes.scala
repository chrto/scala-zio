package zio2demo.web.api

import zio.http.{Routes, HandlerAspect}
import zio.uuid.UUIDGenerator

object ApiRoutes {
  import company.CompanyRoutes
  import zio2demo.controller.CompanyController
  import zio2demo.controller.EmployeeController
  import zio2demo.controller.DepartmentController
  import zio2demo.service.EmployeeService
  import zio2demo.web.middleware.authentication.AuthenticationBearerWithContext

  val routes: Routes[CompanyController & EmployeeController & DepartmentController & UUIDGenerator & EmployeeService, Nothing] =
    (
      company.CompanyRoutes.make ++
      employee.EmployeeRoutes.make ++
      department.DepartmentRoutes.make
    )  @@ AuthenticationBearerWithContext.bearerAuth @@ HandlerAspect.debug
}