package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}
import zio.uuid.types.UUIDv7

import zio2demo.storage.driver.{ConnectionLive}
import zio2demo.model.Company
import zio2demo.model.ApplicationError._
import zio2demo.storage.driver.KeyValueStore

import zio2demo.model.Entity
trait CompanyRepository {
  def exists(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Boolean]
  def insert(company: Company): ZIO[ConnectionLive, ApplicationError, Unit]
  def get(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Company]
  def getAll: ZIO[ConnectionLive, ApplicationError, Vector[Company]]
  def delete(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Unit]
}

case class CompanyRepositoryLive() extends CompanyRepository {
  def exists(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Boolean] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Checking if company with id ${uuid} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Company](uuid))
      .map(_.isDefined)
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(company: Company): ZIO[ConnectionLive, ApplicationError, Unit] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Inserting company with id ${company.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Company](company))
      .tap(_ => ZIO.logDebug(s"Company with id ${company.id} inserted"))

  def get(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Company] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Getting company with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.get[Company](uuid))
      .flatMap{
        case Some(company: Company) => ZIO.succeed(company)
        case _ => ZIO.fail(NotFound(s"Company with id ${uuid} not found!"))
      }

  def getAll: ZIO[ConnectionLive, ApplicationError, Vector[Company]] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Getting all companies using connection with id: ${c.id}"))
      .flatMap(_.getAll[Company])
      .flatMap ((companies: Vector[Entity]) => ZIO.succeed(companies.collect { case company: Company => company }))

  def delete(uuid: UUIDv7): ZIO[ConnectionLive, ApplicationError, Unit] =
    ZIO.service[ConnectionLive]
      .tap((c: ConnectionLive) => ZIO.logDebug(s"Deleting company with id ${uuid} using connection with id: ${c.id}"))
      .flatMap(_.remove[Company](uuid))
}

object CompanyRepositoryLive {
  lazy val live: ULayer[CompanyRepository] = ZLayer.succeed(CompanyRepositoryLive())
}