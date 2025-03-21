package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Department
import zio2demo.model.ApplicationError._
import zio2demo.storage.driver.KeyValueStore

import zio2demo.model.Entity
import zio.uuid.types.UUIDv7
trait DepartmentRepository {
  def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean]
  def insert(department: Department): ZIO[Connection, ApplicationError, Unit]
  def getUnsafe(uuid: UUIDv7): ZIO[Connection, ApplicationError, Department]
  def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Department]
  def getAllUnsafe: ZIO[Connection, ApplicationError, Seq[Department]]
  def getAll: ZIO[Connection, ApplicationError, Seq[Department]]
  def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit]
}

case class DepartmentRepositoryLive() extends DepartmentRepository {
  def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Checking if department with id ${uuid} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Department](uuid))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Department with id ${uuid} exists")
        case false => ZIO.logDebug(s"Department with id ${uuid} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(department: Department): ZIO[Connection, ApplicationError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Inserting department with id ${department.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Department](department))
      .tap(_ => ZIO.logDebug(s"Department with id ${department.id} inserted"))

  def getUnsafe(uuid: UUIDv7): ZIO[Connection, ApplicationError, Department] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting department with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.getUnsafe[Department](uuid))
      .flatMap(_.fold
        (ZIO.fail(NotFound(s"Department with id ${uuid} not found!")))
        (ZIO.succeed)
      )

  def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Department] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting department with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.get[Department](uuid))
      .flatMap(_.fold
        (ZIO.fail(NotFound(s"Department with id ${uuid} not found!")))
        (ZIO.succeed)
      )

  def getAllUnsafe: ZIO[Connection, ApplicationError, Seq[Department]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting all departments using connection with id: ${c.id}"))
      .flatMap(_.getAllUnsafe[Department])

  def getAll: ZIO[Connection, ApplicationError, Seq[Department]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting all departments using connection with id: ${c.id}"))
      .flatMap(_.getAll[Department])

  def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Deleting department with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.remove[Department](uuid))
}

object DepartmentRepositoryLive {
  lazy val live: ULayer[DepartmentRepository] = ZLayer.succeed(DepartmentRepositoryLive())
}