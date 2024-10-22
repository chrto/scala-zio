package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Employee
import zio2demo.model.error.{DatabaseError, NotFound}
import zio2demo.storage.driver.KeyValueStore

import scala.util.chaining._
import zio2demo.model.Entity
trait EmployeeRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean]
  def insert(employee: Employee): ZIO[Connection, DatabaseError, Unit]
  def get(id: Int): ZIO[Connection, DatabaseError, Employee]
  def getAll: ZIO[Connection, DatabaseError, Vector[Employee]]
  def delete(id: Int): ZIO[Connection, DatabaseError, Unit]
}

case class EmployeeRepositoryLive() extends EmployeeRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Checking if employee with id ${id} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Employee](id))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Employee with id ${id} exists")
        case false => ZIO.logInfo(s"Employee with id ${id} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(employee: Employee): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Inserting employee with id ${employee.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Employee](employee))
      .tap(_ => ZIO.logInfo(s"Employee with id ${employee.id} inserted"))

  def get(id: Int): ZIO[Connection, DatabaseError, Employee] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting employee with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.get[Employee](id))
      .flatMap{
        case Some(employee: Employee) => ZIO.succeed(employee)
        case _ => ZIO.fail(NotFound(s"No employee found with id ${id}"))
      }

  def getAll: ZIO[Connection, DatabaseError, Vector[Employee]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting all employees using connection with id: ${c.id}"))
      .flatMap(_.getAll[Employee])
      .flatMap ((employees: Vector[Entity]) => ZIO.succeed(employees.collect { case employee: Employee => employee }))

  def delete(id: Int): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Deleting employee with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.remove[Employee](id))
}

object EmployeeRepositoryLive {
  lazy val live: ULayer[EmployeeRepository] = ZLayer.succeed(EmployeeRepositoryLive())
}