package zio2demo.controller

import zio.{ZIO, URLayer, ZLayer, IO}
import zio.uuid.UUIDGenerator
import zio.uuid.types.UUIDv7

import zio2demo.service.DepartmentService
import zio2demo.model.Department
import zio2demo.model.ApplicationError._

// Define service
trait DepartmentController {
  def getDepartment(uuid: UUIDv7): IO[ApplicationError, Department]
  def getDepartments: IO[ApplicationError, Vector[Department]]
  def addDepartment(name: String, companyId: UUIDv7): ZIO[UUIDGenerator, ApplicationError, UUIDv7]
  def deleteDepartment(uuid: UUIDv7): IO[ApplicationError, UUIDv7]
}

// front-facing API (Accessor Methods)
object DepartmentController {
  def getDepartment(uuid: UUIDv7): ZIO[DepartmentController, ApplicationError, Department] =
    ZIO.serviceWithZIO[DepartmentController](_.getDepartment(uuid))

  def getDepartments: ZIO[DepartmentController, ApplicationError, Vector[Department]] =
    ZIO.serviceWithZIO[DepartmentController](_.getDepartments)

  def addDepartment(name: String, companyId: UUIDv7): ZIO[DepartmentController & UUIDGenerator, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[DepartmentController](_.addDepartment(name, companyId))

  def deleteDepartment(uuid: UUIDv7): ZIO[DepartmentController, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[DepartmentController](_.deleteDepartment(uuid))
}

//  Implement service
case class DepartmentControllerLive(departmentService: DepartmentService) extends DepartmentController {
  def getDepartment(uuid: UUIDv7): IO[ApplicationError, Department] =
    departmentService.get(uuid)

  def getDepartments: IO[ApplicationError, Vector[Department]] =
    departmentService.getAll

  def addDepartment(name: String, companyID: UUIDv7): ZIO[UUIDGenerator, ApplicationError, UUIDv7] =
    ZIO.serviceWithZIO[UUIDGenerator](_.uuidV7)
      .flatMap((uuid: UUIDv7) => departmentService.add(Department(uuid, name, companyID)).as[UUIDv7](uuid))

  def deleteDepartment(uuid: UUIDv7): IO[ApplicationError, UUIDv7] =
    departmentService.delete(uuid)
      .as[UUIDv7](uuid)
}

// lift the service implementation into the ZLayer
object DepartmentControllerLive {
  val live: URLayer[DepartmentService, DepartmentController] = ZLayer.fromFunction(DepartmentControllerLive(_))
  // same as:
  val live2: URLayer[DepartmentService, DepartmentController] =
    ZLayer {
      for {
        departmentService <- ZIO.service[DepartmentService]
      } yield DepartmentControllerLive(departmentService)
    }
  // same as:
  val live3: URLayer[DepartmentService, DepartmentController]  =
    ZLayer(ZIO.service[DepartmentService].map(DepartmentControllerLive(_)))
}