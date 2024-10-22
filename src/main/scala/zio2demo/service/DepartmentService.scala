package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio2demo.model.Department
import zio2demo.storage.repositories.DepartmentRepository
import zio2demo.storage.Database
import zio2demo.model.error.{EntityExistsError, ServiceError, DatabaseError}

trait DepartmentService {
  def add(department: Department): IO[DatabaseError | ServiceError, Unit]
}

object DepartmentService {
  def add(department: Department): ZIO[DepartmentService, DatabaseError | ServiceError, Unit] =
    ZIO.serviceWith[DepartmentService](_.add(department))
}

case class DepartmentServiceLive(departmentRepository: DepartmentRepository, db: Database) extends DepartmentService {
  def add(department: Department): IO[DatabaseError | ServiceError, Unit] =
    db.transact(
      departmentRepository.exists(department.id)
        .flatMap{
          case true  => ZIO.fail(EntityExistsError(department.id))
          case false => departmentRepository.insert(department)
        }
    )
}

object DepartmentServiceLive {
  val live: URLayer[DepartmentRepository & Database, DepartmentService] = ZLayer.fromFunction(DepartmentServiceLive(_, _))
  // same as:
  val live2: URLayer[DepartmentRepository & Database, DepartmentService] =
    ZLayer {
      for {
        departmentRepository <- ZIO.service[DepartmentRepository]
        db <- ZIO.service[Database]
      } yield DepartmentServiceLive(departmentRepository, db)
    }
  // same as:
  val live3: URLayer[DepartmentRepository & Database, DepartmentService]  = ZLayer{
    ZIO.service[DepartmentRepository].zipWith(ZIO.service[Database])(DepartmentServiceLive(_, _))
  }
  // same as:
  val live4: URLayer[DepartmentRepository & Database, DepartmentService] = ZLayer{
    (ZIO.service[DepartmentRepository] <*> ZIO.service[Database]).map(DepartmentServiceLive(_, _))
  }
}