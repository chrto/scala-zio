package zio2demo

import zio.{ZIOAppArgs, ZIO, ZLayer, ZIOAppDefault, Scope, URIO}
import zio.uuid.UUIDGenerator

import zio2demo.controller.{EmployeeControllerLive, DepartmentControllerLive, CompanyControllerLive}
import zio2demo.controller.{EmployeeController, DepartmentController, CompanyController}
import zio2demo.service.{EmployeeServiceLive, DepartmentServiceLive, CompanyServiceLive}
import zio2demo.storage.{DatabaseLive}
import zio2demo.storage.driver.{ConnectionPoolLive}
import zio2demo.storage.repositories.{EmployeeRepositoryLive, DepartmentRepositoryLive, CompanyRepositoryLive}

object MyDBApp extends ZIOAppDefault {
  import zio2demo.common.Crypto._

  def program: URIO[UUIDGenerator & EmployeeController & DepartmentController & CompanyController, Unit] = (
    for {
      tescosUuid <- CompanyController.addCompany("Tescos")
      _ <- CompanyController.addCompany("Deskos")
      _ <- CompanyController.addCompany("Plenkos")

      devUuid <- DepartmentController.addDepartment("DEV", tescosUuid)
      _ <- DepartmentController.addDepartment("SALE", tescosUuid)
      _ <- DepartmentController.addDepartment("DESK", tescosUuid)

      _ <- EmployeeController.addEmployee("John Doe", "joe.doe@company.com", hashPwd("joe-123"), devUuid)
      _ <- EmployeeController.addEmployee("Jack Black", "jack.black@company.com", hashPwd("jack-123"), devUuid)
      _ <- EmployeeController.addEmployee("Admin Adminovic", "admin.adminovic@company.com", hashPwd("admin-123"), devUuid)
    } yield ()
  ).catchAll(e => ZIO.logError(e.toString))

  val myLayer: ZLayer[Any, Nothing, EmployeeController & DepartmentController & CompanyController] =
    ((EmployeeRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> EmployeeServiceLive.live >>> EmployeeControllerLive.live)
    ++ ((DepartmentRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> DepartmentServiceLive.live >>> DepartmentControllerLive.live)
    ++ ((CompanyRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> CompanyServiceLive.live >>> CompanyControllerLive.live)

  def run: ZIO[ZIOAppArgs & Scope, Nothing, Any] = program.provide(myLayer, UUIDGenerator.live).exitCode
}

