package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}
import zio.uuid.types.UUIDv7

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Company
import zio2demo.model.ApplicationError._
import zio2demo.storage.driver.KeyValueStore

import zio2demo.model.Entity
trait CompanyRepository {
  def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean]
  def insert(company: Company): ZIO[Connection, ApplicationError, Unit]
  def getUnsafe(uuid: UUIDv7): ZIO[Connection, ApplicationError, Company]
  def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Company]
  def getAllUnsafe: ZIO[Connection, ApplicationError, Seq[Company]]
  def getAll: ZIO[Connection, ApplicationError, Seq[Company]]
  def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit]
}

case class CompanyRepositoryLive() extends CompanyRepository {
  def exists(uuid: UUIDv7): ZIO[Connection, ApplicationError, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Checking if company with id ${uuid} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Company](uuid))
      .map(_.isDefined)
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(company: Company): ZIO[Connection, ApplicationError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Inserting company with id ${company.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Company](company))
      .tap(_ => ZIO.logDebug(s"Company with id ${company.id} inserted"))

  def getUnsafe(uuid: UUIDv7): ZIO[Connection, ApplicationError, Company] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting company with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.getUnsafe[Company](uuid))
      .flatMap(_.fold
        (ZIO.fail(NotFound(s"Company with id ${uuid} not found!")))
        (ZIO.succeed)
      )

  def get(uuid: UUIDv7): ZIO[Connection, ApplicationError, Company] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting company with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.get[Company](uuid))
      .flatMap(_.fold
        (ZIO.fail(NotFound(s"Company with id ${uuid} not found!")))
        (ZIO.succeed)
      )

  def getAllUnsafe: ZIO[Connection, ApplicationError, Seq[Company]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting all companies using connection with id: ${c.id}"))
      .flatMap(_.getAllUnsafe[Company])

  def getAll: ZIO[Connection, ApplicationError, Seq[Company]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Getting all companies using connection with id: ${c.id}"))
      .flatMap(_.getAll[Company])

  def delete(uuid: UUIDv7): ZIO[Connection, ApplicationError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logDebug(s"Deleting company with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.remove[Company](uuid))
}

object CompanyRepositoryLive {
  lazy val live: ULayer[CompanyRepository] = ZLayer.succeed(CompanyRepositoryLive())
}