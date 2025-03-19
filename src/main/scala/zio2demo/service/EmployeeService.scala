package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio.uuid.types.UUIDv7

import zio2demo.model.ApplicationError._
import zio2demo.model.Employee
import zio2demo.storage.repositories.EmployeeRepository
import zio2demo.storage.Database

trait EmployeeService {
  def get(uuid: UUIDv7): IO[ApplicationError, Employee]
  def getByEmail(email: String): IO[ApplicationError, Employee]
  def getByCredentials(userName: String, pwdHash: String): IO[ApplicationError, Employee]
  def getAll: IO[ApplicationError, Seq[Employee]]
  def add(employee: Employee): IO[ApplicationError, Unit]
  def delete(uuid: UUIDv7): IO[ApplicationError, Unit]
}

object EmployeeService {
  def get(uuid: UUIDv7): ZIO[EmployeeService, ApplicationError, Employee] =
    ZIO.serviceWithZIO[EmployeeService](_.get(uuid))

  def getByEmail(email: String): ZIO[EmployeeService, ApplicationError, Employee] =
    ZIO.serviceWithZIO[EmployeeService](_.getByEmail(email))

  def getByCredentials(userName: String, pwdHash: String): ZIO[EmployeeService, ApplicationError, Employee] =
    ZIO.serviceWithZIO[EmployeeService](_.getByCredentials(userName, pwdHash))

  def getAll: ZIO[EmployeeService, ApplicationError, Seq[Employee]] =
    ZIO.serviceWithZIO[EmployeeService](_.getAll)

  def add(employee: Employee): ZIO[EmployeeService, ApplicationError, Unit] =
    ZIO.serviceWithZIO[EmployeeService](_.add(employee))

  def delete(uuid: UUIDv7): ZIO[EmployeeService, ApplicationError, Unit] =
    ZIO.serviceWithZIO[EmployeeService](_.delete(uuid))
}

case class EmployeeServiceLive(employeeRepository: EmployeeRepository, db: Database) extends EmployeeService {
  def get(uuid: UUIDv7): IO[ApplicationError, Employee] =
    db.transact(
      employeeRepository.get(uuid)
    )

  def getByEmail(email: String): IO[ApplicationError, Employee] =
    db.transact(
      employeeRepository.find(_.email == email)
      .catchSome{
        case _: NotFound => ZIO.fail(NotFound(s"No employee found with email ${email}!"))
      }
    )

  def getByCredentials(userName: String, pwdHash: String): IO[ApplicationError, Employee] =
    db.transact(
      employeeRepository.find(_.email == userName)
        .catchSome{
          case _: NotFound => ZIO.fail(Unauthenticated(s"No employee found with email ${userName}!"))
        }
        .flatMap{ (employee: Employee) =>
          employee.pwdHash == pwdHash match
            case true => ZIO.succeed(employee)
            case false => ZIO.fail(Unauthenticated(s"Invalid password for employee with email ${userName}!"))
        }
    )

  def getAll: IO[ApplicationError, Seq[Employee]] =
    db.transact(
      employeeRepository.getAll
    )

  def add(employee: Employee): IO[ApplicationError, Unit] =
    db.transact(
      employeeRepository.exists(employee.id)
        .flatMap{
          case false => employeeRepository.insert(employee)
          case true  =>
            ZIO.logWarning(s"Employee with id ${employee.id} exists!") *>
              ZIO.fail(BadRequest(s"Employee with id ${employee.id} already exists!"))
        }
    )

  def delete(uuid: UUIDv7): IO[ApplicationError, Unit] =
    db.transact(
      employeeRepository.exists(uuid)
        .flatMap{
          case true  => employeeRepository.delete(uuid)
          case false  =>
            ZIO.logWarning(s"Employee with id ${uuid} does not exist!") *>
              ZIO.fail(NotFound(s"Employee with id ${uuid} does not exists!"))
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