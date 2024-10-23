package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio2demo.model.Employee
import zio2demo.storage.repositories.EmployeeRepository
import zio2demo.storage.Database
import zio2demo.model.error.{EntityExistsError, ServiceError, DatabaseError}

trait EmployeeService {
  def add(employee: Employee): IO[DatabaseError | ServiceError, Unit]
}

object EmployeeService {
  def add(employee: Employee): ZIO[EmployeeService, DatabaseError | ServiceError, Unit] =
    ZIO.serviceWith[EmployeeService](_.add(employee))
}

case class EmployeeServiceLive(employeeRepository: EmployeeRepository, db: Database) extends EmployeeService {
  def add(employee: Employee): IO[DatabaseError | ServiceError, Unit] =
    db.transact(
      employeeRepository.exists(employee.id)
        .flatMap{
          case true  => ZIO.fail(EntityExistsError(employee.id))
          case false => employeeRepository.insert(employee)
        }
    )
}

object EmployeeServiceLive {
  val live: URLayer[EmployeeRepository & Database, EmployeeService] = ZLayer.fromFunction(EmployeeServiceLive(_, _))
  // same as:
  val live2: URLayer[EmployeeRepository & Database, EmployeeService] =
    ZLayer {
      for {
        employeeRepository <- ZIO.service[EmployeeRepository]
        db <- ZIO.service[Database]
      } yield EmployeeServiceLive(employeeRepository, db)
    }
  // same as:
  val live3: URLayer[EmployeeRepository & Database, EmployeeService]  = ZLayer{
    ZIO.service[EmployeeRepository].zipWith(ZIO.service[Database])(EmployeeServiceLive(_, _))
  }
  // same as:
  val live4: URLayer[EmployeeRepository & Database, EmployeeService] = ZLayer{
    (ZIO.service[EmployeeRepository] <*> ZIO.service[Database]).map(EmployeeServiceLive(_, _))
  }
}