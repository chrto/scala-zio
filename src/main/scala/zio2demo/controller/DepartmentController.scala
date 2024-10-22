package zio2demo.controller

import zio.{ZIO, URLayer, ZLayer, UIO, URIO}

import scala.util.chaining._

import zio2demo.service.DepartmentService
import zio2demo.model.Department
import zio2demo.model.error.{ServiceError, EntityExistsError}
import zio2demo.model.error.{DatabaseError, ConnectionNotAvailable}

// Define service
trait DepartmentController {
  def addDepartment(id: Int, name: String, companyId: Int): UIO[String]
}

// front-facing API (Accessor Methods)
object DepartmentController {
  def addDepartment(id: Int, name: String, companyId: Int): URIO[DepartmentController, String] =
    ZIO.serviceWithZIO[DepartmentController](_.addDepartment(id, name, companyId))
}

//  Implement service
case class DepartmentControllerLive(departmentService: DepartmentService) extends DepartmentController {
  def addDepartment(id: Int, name: String, companyId: Int): UIO[String] = {
    Department(id, name, companyId)
      .pipe(departmentService.add)
      .as("Department registered successfully")
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