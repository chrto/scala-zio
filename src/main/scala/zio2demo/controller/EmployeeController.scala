package zio2demo.controller

import zio.{ZIO, URLayer, ZLayer, UIO, URIO}

import scala.util.chaining._

import zio2demo.service.EmployeeService
import zio2demo.model.Employee
import zio2demo.model.error.{ServiceError, EntityExistsError}
import zio2demo.model.error.{DatabaseError, ConnectionNotAvailable}

// Define service
trait EmployeeController {
  def addEmployee(id: Int, name: String, departmentId: Int): UIO[String]
}

// front-facing API (Accessor Methods)
object EmployeeController {
  def addEmployee(id: Int, name: String, departmentId: Int): URIO[EmployeeController, String] =
    ZIO.serviceWithZIO[EmployeeController](_.addEmployee(id, name, departmentId))
}

//  Implement service
case class EmployeeControllerLive(employeeService: EmployeeService) extends EmployeeController {
  def addEmployee(id: Int, name: String, departmentId: Int): UIO[String] = {
    Employee(id, name, departmentId)
      .pipe(employeeService.add)
      .as("Employee registered successfully")
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
object EmployeeControllerLive {
  val live: URLayer[EmployeeService, EmployeeController] = ZLayer.fromFunction(EmployeeControllerLive(_))
  // same as:
  val live2: URLayer[EmployeeService, EmployeeController] =
    ZLayer {
      for {
        employeeService <- ZIO.service[EmployeeService]
      } yield EmployeeControllerLive(employeeService)
    }
  // same as:
  val live3: URLayer[EmployeeService, EmployeeController]  =
    ZLayer(ZIO.service[EmployeeService].map(EmployeeControllerLive(_)))
}