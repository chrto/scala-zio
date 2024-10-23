package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Department
import zio2demo.model.error.{DatabaseError, NotFound}
import zio2demo.storage.driver.KeyValueStore

import scala.util.chaining._
import zio2demo.model.Entity
trait DepartmentRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean]
  def insert(department: Department): ZIO[Connection, DatabaseError, Unit]
  def get(id: Int): ZIO[Connection, DatabaseError, Department]
  def getAll: ZIO[Connection, DatabaseError, Vector[Department]]
  def delete(id: Int): ZIO[Connection, DatabaseError, Unit]
}

case class DepartmentRepositoryLive() extends DepartmentRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Checking if department with id ${id} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Department](id))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Department with id ${id} exists")
        case false => ZIO.logInfo(s"Department with id ${id} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(department: Department): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Inserting department with id ${department.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Department](department))
      .tap(_ => ZIO.logInfo(s"Department with id ${department.id} inserted"))

  def get(id: Int): ZIO[Connection, DatabaseError, Department] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting department with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.get[Department](id))
      .flatMap{
        case Some(department: Department) => ZIO.succeed(department)
        case _ => ZIO.fail(NotFound(s"No department found with id ${id}"))
      }

  def getAll: ZIO[Connection, DatabaseError, Vector[Department]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting all departments using connection with id: ${c.id}"))
      .flatMap(_.getAll[Department])
      .flatMap ((departments: Vector[Entity]) => ZIO.succeed(departments.collect { case department: Department => department }))

  def delete(id: Int): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Deleting department with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.remove[Department](id))
}

object DepartmentRepositoryLive {
  lazy val live: ULayer[DepartmentRepository] = ZLayer.succeed(DepartmentRepositoryLive())
}