package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}

import zio2demo.storage.driver.{ConnectionLive}
import zio2demo.model.Department
import zio2demo.model.ApplicationError._
import zio2demo.storage.driver.KeyValueStore

import zio2demo.model.Entity
import zio.uuid.types.UUIDv7
trait DepartmentRepository {
  def exists(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Boolean]
  def insert(department: Department): ZIO[ConnectionLive, ApplicationError, Unit]
  def get(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Department]
  def getAll: ZIO[ConnectionLive, ApplicationError, Vector[Department]]
  def delete(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Unit]
}

case class DepartmentRepositoryLive() extends DepartmentRepository {
  def exists(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Boolean] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Checking if department with id ${uuid} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Department](uuid))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Department with id ${uuid} exists")
        case false => ZIO.logDebug(s"Department with id ${uuid} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(department: Department): ZIO[ConnectionLive, ApplicationError, Unit] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Inserting department with id ${department.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Department](department))
      .tap(_ => ZIO.logDebug(s"Department with id ${department.id} inserted"))

  def get(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Department] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Getting department with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.get[Department](uuid))
      .flatMap{
        case Some(department: Department) => ZIO.succeed(department)
        case _ => ZIO.fail(NotFound(s"No department found with id ${uuid}"))
      }

  def getAll: ZIO[ConnectionLive, ApplicationError, Vector[Department]] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Getting all departments using connection with id: ${c.id}"))
      .flatMap(_.getAll[Department])
      .flatMap ((departments: Vector[Entity]) => ZIO.succeed(departments.collect { case department: Department => department }))

  def delete(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Unit] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Deleting department with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.remove[Department](uuid))
}

object DepartmentRepositoryLive {
  lazy val live: ULayer[DepartmentRepository] = ZLayer.succeed(DepartmentRepositoryLive())
}