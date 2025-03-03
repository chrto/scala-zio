package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}
import zio.uuid.types.UUIDv7

import zio2demo.storage.driver.{ConnectionLive}
import zio2demo.model.Employee
import zio2demo.model.ApplicationError._
import zio2demo.storage.driver.KeyValueStore

import zio2demo.model.Entity

trait EmployeeRepository {
  def exists(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Boolean]
  def insert(employee: Employee): ZIO[ConnectionLive, ApplicationError, Unit]
  def get(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Employee]
  def getAll: ZIO[ConnectionLive, ApplicationError, Vector[Employee]]
  def find(predicate: Employee => Boolean): ZIO[ConnectionLive, ApplicationError, Employee]
  def delete(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Unit]
}

case class EmployeeRepositoryLive() extends EmployeeRepository {
  def exists(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Boolean] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Checking if employee with id ${uuid} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Employee](uuid))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Employee with id ${uuid} exists")
        case false => ZIO.logDebug(s"Employee with id ${uuid} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(employee: Employee): ZIO[ConnectionLive, ApplicationError, Unit] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Inserting employee with id ${employee.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Employee](employee))
      .tap(_ => ZIO.logDebug(s"Employee with id ${employee.id} inserted"))

  def get(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Employee] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Getting employee with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.get[Employee](uuid))
      .flatMap{
        case Some(employee: Employee) => ZIO.succeed(employee)
        case _ => ZIO.fail(NotFound(s"No employee found with id ${uuid}"))
      }

  def getAll: ZIO[ConnectionLive, ApplicationError, Vector[Employee]] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Getting all employees using connection with id: ${c.id}"))
      .flatMap(_.getAll[Employee])
      .flatMap ((employees: Vector[Entity]) => ZIO.succeed(employees.collect { case employee: Employee => employee }))

  def find(predicate: Employee => Boolean): ZIO[ConnectionLive, ApplicationError, Employee] =
    getAll
      .flatMap(_.find(predicate) match {
        case Some(employee) => ZIO.succeed(employee)
        case None => ZIO.fail(NotFound(s"No employee found!"))
      })

  def delete(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Unit] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Deleting employee with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.remove[Employee](uuid))
}

object EmployeeRepositoryLive {
  lazy val live: ULayer[EmployeeRepository] = ZLayer.succeed(EmployeeRepositoryLive())
}