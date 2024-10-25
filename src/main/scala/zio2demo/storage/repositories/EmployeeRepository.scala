package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}
import zio.uuid.types.UUIDv7

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Employee
import zio2demo.model.ApplicationError._
import zio2demo.storage.driver.KeyValueStore

import zio2demo.model.Entity

trait EmployeeRepository {
  def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean]
  def insert(employee: Employee): ZIO[Connection, ApplicationError, Unit]
  def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Employee]
  def getAll: ZIO[Connection, ApplicationError, Vector[Employee]]
  def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit]
}

case class EmployeeRepositoryLive() extends EmployeeRepository {
  def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Checking if employee with id ${uuid} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Employee](uuid))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Employee with id ${uuid} exists")
        case false => ZIO.logDebug(s"Employee with id ${uuid} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(employee: Employee): ZIO[Connection, ApplicationError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Inserting employee with id ${employee.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Employee](employee))
      .tap(_ => ZIO.logDebug(s"Employee with id ${employee.id} inserted"))

  def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Employee] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting employee with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.get[Employee](uuid))
      .flatMap{
        case Some(employee: Employee) => ZIO.succeed(employee)
        case _ => ZIO.fail(NotFound(s"No employee found with id ${uuid}"))
      }

  def getAll: ZIO[Connection, ApplicationError, Vector[Employee]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting all employees using connection with id: ${c.id}"))
      .flatMap(_.getAll[Employee])
      .flatMap ((employees: Vector[Entity]) => ZIO.succeed(employees.collect { case employee: Employee => employee }))

  def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Deleting employee with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.remove[Employee](uuid))
}

object EmployeeRepositoryLive {
  lazy val live: ULayer[EmployeeRepository] = ZLayer.succeed(EmployeeRepositoryLive())
}