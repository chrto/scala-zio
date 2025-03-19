package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio.uuid.types.UUIDv7

import zio2demo.model.Department
import zio2demo.storage.repositories.DepartmentRepository
import zio2demo.storage.Database
import zio2demo.model.ApplicationError._

trait DepartmentService {
  def get(uuid: UUIDv7): IO[ApplicationError, Department]
  def getAll: IO[ApplicationError, Seq[Department]]
  def add(department: Department): IO[ApplicationError, Unit]
  def delete(uuid: UUIDv7): IO[ApplicationError, Unit]
}

object DepartmentService {
  def get(uuid: UUIDv7): ZIO[DepartmentService, ApplicationError, Department] =
    ZIO.serviceWithZIO[DepartmentService](_.get(uuid))

  def getAll: ZIO[DepartmentService, ApplicationError, Seq[Department]] =
    ZIO.serviceWithZIO[DepartmentService](_.getAll)

  def add(department: Department): ZIO[DepartmentService, ApplicationError, Unit] =
    ZIO.serviceWithZIO[DepartmentService](_.add(department))

  def delete(uuid: UUIDv7): ZIO[DepartmentService, ApplicationError, Unit] =
    ZIO.serviceWithZIO[DepartmentService](_.delete(uuid))
}

case class DepartmentServiceLive(departmentRepository: DepartmentRepository, db: Database) extends DepartmentService {
  def get(uuid: UUIDv7): IO[ApplicationError, Department] =
    db.transact(
      departmentRepository.get(uuid)
    )

  def getAll: IO[ApplicationError, Seq[Department]] =
    db.transact(
      departmentRepository.getAll
    )

  def add(department: Department): IO[ApplicationError, Unit] =
    db.transact(
      departmentRepository.exists(department.id)
        .flatMap{
          case false => departmentRepository.insert(department)
          case true  =>
            ZIO.logWarning(s"Department with id ${department.id} exists!") *>
              ZIO.fail(BadRequest(s"Department with id ${department.id} already exists!"))
        }
    )

  def delete(uuid: UUIDv7): IO[ApplicationError, Unit] =
    db.transact(
      departmentRepository.exists(uuid)
        .flatMap{
          case true => departmentRepository.delete(uuid)
          case false  =>
            ZIO.logWarning(s"Department with id ${uuid} does not exist!") *>
              ZIO.fail(BadRequest(s"Department with id ${uuid} does not exist!"))
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