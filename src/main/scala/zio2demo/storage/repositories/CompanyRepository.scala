package zio2demo.storage.repositories

import zio.{URIO, ZIO, ZLayer, ULayer, Ref, IO}

import zio2demo.storage.driver.{Connection}
import zio2demo.model.Company
import zio2demo.model.error.{DatabaseError, NotFound}
import zio2demo.storage.driver.KeyValueStore

import scala.util.chaining._
import zio2demo.model.Entity
trait CompanyRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean]
  def insert(company: Company): ZIO[Connection, DatabaseError, Unit]
  def get(id: Int): ZIO[Connection, DatabaseError, Company]
  def getAll: ZIO[Connection, DatabaseError, Vector[Company]]
  def delete(id: Int): ZIO[Connection, DatabaseError, Unit]
}

case class CompanyRepositoryLive() extends CompanyRepository {
  def exists(id: Int): ZIO[Connection, DatabaseError, Boolean] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Checking if company with id ${id} exists using connection with id: ${c.id}"))
      .flatMap(_.get[Company](id))
      .map(_.isDefined)
      .tap {
        case true => ZIO.logWarning(s"Company with id ${id} exists")
        case false => ZIO.logInfo(s"Company with id ${id} does not exist")
      }
      .catchSome{ case _: NotFound => ZIO.succeed(false) }

  def insert(company: Company): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Inserting company with id ${company.id} using connection with id: ${c.id}"))
      .flatMap(_.add[Company](company))
      .tap(_ => ZIO.logInfo(s"Company with id ${company.id} inserted"))

  def get(id: Int): ZIO[Connection, DatabaseError, Company] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting company with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.get[Company](id))
      .flatMap{
        case Some(company: Company) => ZIO.succeed(company)
        case _ => ZIO.fail(NotFound(s"No company found with id ${id}"))
      }

  def getAll: ZIO[Connection, DatabaseError, Vector[Company]] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Getting all companies using connection with id: ${c.id}"))
      .flatMap(_.getAll[Company])
      .flatMap ((companies: Vector[Entity]) => ZIO.succeed(companies.collect { case company: Company => company }))

  def delete(id: Int): ZIO[Connection, DatabaseError, Unit] =
    ZIO.service[Connection]
      .tap((c: Connection) => ZIO.logInfo(s"Deleting company with id ${id} using connection with id: ${c.id}"))
      .flatMap(_.remove[Company](id))
}

object CompanyRepositoryLive {
  lazy val live: ULayer[CompanyRepository] = ZLayer.succeed(CompanyRepositoryLive())
}