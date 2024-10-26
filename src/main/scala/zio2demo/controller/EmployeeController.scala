package zio2demo.controller

import zio.{ZIO, URLayer, ZLayer, IO}
import zio.uuid.types.UUIDv7
import zio.uuid.UUIDGenerator

import zio2demo.service.EmployeeService
import zio2demo.model.Employee
import zio2demo.model.ApplicationError._
import zio2demo.common.Crypto.Crypto

// Define service
trait EmployeeController {
  def getEmployee(uuid: UUIDv7): IO[ApplicationError, Employee]
  def getEmployees: IO[ApplicationError, Vector[Employee]]
  def addEmployee(name: String, email: String, pwdHash: String, departmentId: UUIDv7): ZIO[UUIDGenerator, ApplicationError, UUIDv7]
  def deleteEmployee(uuid: UUIDv7): IO[ApplicationError, UUIDv7]
}

// front-facing API (Accessor Methods)
object EmployeeController {
  def getEmployee(uuid: UUIDv7): ZIO[EmployeeController, ApplicationError, Employee] =
    ZIO.serviceWithZIO[EmployeeController](_.getEmployee(uuid))

  def getEmployees: ZIO[EmployeeController, ApplicationError, Vector[Employee]] =
    ZIO.serviceWithZIO[EmployeeController](_.getEmployees)

  def addEmployee(name: String, email: String, password: String, departmentId: UUIDv7): ZIO[EmployeeController & UUIDGenerator, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[EmployeeController](_.addEmployee(name, email, Crypto.hashPwd(password), departmentId))

  def deleteEmployee(uuid: UUIDv7): ZIO[EmployeeController, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[EmployeeController](_.deleteEmployee(uuid))
}

//  Implement service
case class EmployeeControllerLive(employeeService: EmployeeService) extends EmployeeController {
  def getEmployee(uuid: UUIDv7): IO[ApplicationError, Employee] =
    employeeService.get(uuid)

  def getEmployees: IO[ApplicationError, Vector[Employee]] =
    employeeService.getAll

  def addEmployee(name: String, email: String, pwdHash: String, departmentId: UUIDv7): ZIO[UUIDGenerator, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[UUIDGenerator](_.uuidV7)
      .flatMap((uuid: UUIDv7) => employeeService.add(Employee(uuid, name, email, pwdHash, departmentId)).as[UUIDv7](uuid))

  def deleteEmployee(uuid: UUIDv7): IO[ApplicationError, UUIDv7] =
    employeeService.delete(uuid)
      .as[UUIDv7](uuid)
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