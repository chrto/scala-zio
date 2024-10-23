package zio2demo.service

import zio.{ZIO, ZLayer, URLayer, IO}
import zio2demo.model.Company
import zio2demo.storage.repositories.CompanyRepository
import zio2demo.storage.Database
import zio2demo.model.error.{EntityExistsError, ServiceError, DatabaseError}

trait CompanyService {
  def add(company: Company): IO[DatabaseError | ServiceError, Unit]
}

object CompanyService {
  def add(company: Company): ZIO[CompanyService, DatabaseError | ServiceError, Unit] =
    ZIO.serviceWith[CompanyService](_.add(company))
}

case class CompanyServiceLive(companyRepository: CompanyRepository, db: Database) extends CompanyService {
  def add(company: Company): IO[DatabaseError | ServiceError, Unit] =
    db.transact(
      companyRepository.exists(company.id)
        .flatMap{
          case true  => ZIO.fail(EntityExistsError(company.id))
          case false => companyRepository.insert(company)
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