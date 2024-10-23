package zio2demo

import zio.{ZIOAppArgs, ZIO, ZLayer, ZIOAppDefault, Scope, URIO}

import zio2demo.controller.{CarControllerLive, EmployeeControllerLive, DepartmentControllerLive, CompanyControllerLive}
import zio2demo.controller.{CarController, EmployeeController, DepartmentController, CompanyController}
import zio2demo.service.{CarServiceLive, EmployeeServiceLive, DepartmentServiceLive, CompanyServiceLive}
import zio2demo.storage.{DatabaseLive}
import zio2demo.storage.driver.{ConnectionPoolLive}
import zio2demo.storage.repositories.{CarRepositoryLive, EmployeeRepositoryLive, DepartmentRepositoryLive, CompanyRepositoryLive}
import zio2demo.storage.repositories.CompanyRepository

object MyDBApp extends ZIOAppDefault {

  def program: URIO[CarController & EmployeeController & DepartmentController & CompanyController, Unit] = {
    for {
      _ <- CarController.register(1, "Ford", "Focus", 1)
      _ <- CarController.register(1, "Toyota", "Corolla", 2)
      _ <- CarController.register(3, "Ford", "Focus", 3)
      _ <- EmployeeController.addEmployee(1, "John Doe", 2)
      _ <- EmployeeController.addEmployee(2, "Jack Black", 2)
      _ <- EmployeeController.addEmployee(1, "Admin Adminovic", 2)
      _ <- DepartmentController.addDepartment(1, "DEV", 2)
      _ <- DepartmentController.addDepartment(2, "SALE", 2)
      _ <- DepartmentController.addDepartment(1, "DESK", 2)
      _ <- CompanyController.addCompany(1, "Tescos")
      _ <- CompanyController.addCompany(2, "Deskos")
      _ <- CompanyController.addCompany(1, "Plenkos")
    } yield ()
  }

  val myLayer: ZLayer[Any, Nothing, CarController & EmployeeController & DepartmentController & CompanyController] =
    ((CarRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> CarServiceLive.live >>> CarControllerLive.live)
    ++ ((EmployeeRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> EmployeeServiceLive.live >>> EmployeeControllerLive.live)
    ++ ((DepartmentRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> DepartmentServiceLive.live >>> DepartmentControllerLive.live)
    ++ ((CompanyRepositoryLive.live ++ (ConnectionPoolLive.live >>> DatabaseLive.live)) >>> CompanyServiceLive.live >>> CompanyControllerLive.live)

  def run: ZIO[ZIOAppArgs & Scope, Nothing, Any] = program.provideLayer(myLayer).exitCode
}

