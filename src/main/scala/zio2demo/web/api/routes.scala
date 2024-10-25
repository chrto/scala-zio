package zio2demo.web.api

import zio.http.Routes
import zio.http.Middleware.basicAuth
import zio.http.HandlerAspect
import zio.uuid.UUIDGenerator
import employee.EmployeeRoutes

object ApiRoutes {
  import company.CompanyRoutes
  import zio2demo.controller.CompanyController
  import zio2demo.controller.EmployeeController
  import zio2demo.controller.DepartmentController

  val routes: Routes[CompanyController & EmployeeController & DepartmentController & UUIDGenerator, Nothing] =
    (
      company.CompanyRoutes.make ++
      employee.EmployeeRoutes.make ++
      department.DepartmentRoutes.make
    ) @@ HandlerAspect.debug
}