package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio.uuid.types.UUIDv7

import zio2demo.model.Company
import zio2demo.model.ApplicationError._
import zio2demo.storage.repositories.CompanyRepository
import zio2demo.storage.Database

trait CompanyService {
  def get(uuid: UUIDv7): IO[ApplicationError, Company]
  def getAll: IO[ApplicationError, Seq[Company]]
  def add(company: Company): IO[ApplicationError, Unit]
  def delete(uuid: UUIDv7): IO[ApplicationError, Unit]
}

object CompanyService {
  def get(uuid: UUIDv7): ZIO[CompanyService, ApplicationError, Company] =
    ZIO.serviceWithZIO[CompanyService](_.get(uuid))

  def getAll: ZIO[CompanyService, ApplicationError, Seq[Company]] =
    ZIO.serviceWithZIO[CompanyService](_.getAll)

  def add(company: Company): ZIO[CompanyService, ApplicationError, Unit] =
    ZIO.serviceWithZIO[CompanyService](_.add(company))

  def delete(uuid: UUIDv7): ZIO[CompanyService, ApplicationError, Unit] =
    ZIO.serviceWithZIO[CompanyService](_.delete(uuid))
}

case class CompanyServiceLive(companyRepository: CompanyRepository, db: Database) extends CompanyService {
  def get(uuid: UUIDv7): IO[ApplicationError, Company] =
    db.transact(
      companyRepository.get(uuid)
    )

  def getAll: IO[ApplicationError, Seq[Company]] =
    db.transact(
      companyRepository.getAll
    )

  def add(company: Company): IO[ApplicationError, Unit] =
    db.transact(
      companyRepository.exists(company.id)
        .flatMap{
          case false => companyRepository.insert(company)
          case true  =>
            ZIO.logWarning(s"Company with id ${company.id} exists!") *>
              ZIO.fail(BadRequest(s"Company with id ${company.id} already exists!"))
        }
    )

  def delete(uuid: UUIDv7): IO[ApplicationError, Unit] =
    db.transact(
      companyRepository.exists(uuid)
        .flatMap{
          case true => companyRepository.delete(uuid)
          case false  =>
            ZIO.logWarning(s"Company with id ${uuid} does not exist!") *>
              ZIO.fail(NotFound(s"Company with id ${uuid} does not exists!"))
        }
    )
}

object CompanyServiceLive {
  val live: URLayer[CompanyRepository & Database, CompanyService] = ZLayer.fromFunction(CompanyServiceLive(_, _))
  // same as:
  val live2: URLayer[CompanyRepository & Database, CompanyService] =
    ZLayer {
      for {
        companyRepository <- ZIO.service[CompanyRepository]
        db <- ZIO.service[Database]
      } yield CompanyServiceLive(companyRepository, db)
    }
  // same as:
  val live3: URLayer[CompanyRepository & Database, CompanyService]  = ZLayer{
    ZIO.service[CompanyRepository].zipWith(ZIO.service[Database])(CompanyServiceLive(_, _))
  }
  // same as:
  val live4: URLayer[CompanyRepository & Database, CompanyService] = ZLayer{
    (ZIO.service[CompanyRepository] <*> ZIO.service[Database]).map(CompanyServiceLive(_, _))
  }
}