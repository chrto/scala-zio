package zio2demo.storage

import zio.{ZIO, IO, UIO, Scope, Ref, ULayer, ZLayer}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7
import cats.syntax.applicative._

object DatabaseSpec extends ZIOSpecDefault {
  import zio2demo.model
  import zio2demo.storage

  import model.ApplicationError.ApplicationError
  import model.{Company, Entity, EntityType}
  import storage.driver.{KeyValueStore, KeyValueStoreLive, Connection, ConnectionPool, ConnectionLive}
  import storage.repositories.CompanyRepositoryLive

  val uuid: UUIDv7 = UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
  val company = Company(uuid, "Company s.r.o")
  val connection = ConnectionMock("connection-1")

  object CounterMock {
    val layer: ZLayer[Any, Nothing, Ref[Vector[(String, Option[String], Option[String])]]] =
      ZLayer.fromZIO(Ref.make(Vector.empty[(String, Option[String], Option[String])]))
  }
  case class ConnectionMock(id: String) extends Connection {
    def add[E <: Entity](value: E)(using entity: EntityType[E]): IO[ApplicationError, Unit] = ???

    def get[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Option[E]] =
      ZIO.succeed[Option[E]](company.asInstanceOf[E].pure[Option])

    def getAll[E <: Entity](using entity: EntityType[E]): IO[ApplicationError, Vector[E]] = ???
    def remove[E <: Entity](uuid: UUIDv7)(using entity: EntityType[E]): IO[ApplicationError, Unit] = ???
  }


  case class ConnectionPoolMock(mCnt: Ref[Vector[(String, Option[String], Option[String])]]) extends ConnectionPool {
    def borrow: IO[ApplicationError, Connection] =
      mCnt.update(_ :+ ("borrow", None, connection.id.pure[Option])) *> ZIO.succeed(connection)

    def release(c: Connection): UIO[Unit] =
      mCnt.update(_ :+ ("release", c.id.pure[Option], None) ) *> ZIO.succeed(())
  }

  object ConnectionPoolMock {
    val layer: ZLayer[Any, Nothing, ConnectionPoolMock] =
      CounterMock.layer >>> ZLayer.fromFunction(ConnectionPoolMock(_))
  }

  object DatabaseSpec {
    val layer: ZLayer[Any, Nothing, Database] =
      ConnectionPoolMock.layer >>>
        DatabaseLive.live
  }

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = {
    suiteAll("Database") {
      suite("transact")(
        for {
          result <-  ZIO.service[Database].flatMap(_.transact(CompanyRepositoryLive().get(uuid)))
          counter <- ZIO.service[Ref[Vector[(String, Option[String], Option[String])]]].flatMap(_.get)
        } yield zio.Chunk(
          suite("TEST")(
            test("Should run dbProgram and return exact entity"){assert(result)(equalTo(company))}
          ),
          suite("SPEC")(
            test("Should call 'borrow' methode form ConnectionPool once"){assert(counter.map(_._1).filter(_ == "borrow").length)(equalTo(1))},
            test("Should call 'release' methode form ConnectionPool once"){assert(counter.map(_._1).filter(_ == "release").length)(equalTo(1))},
            test("Should call 'borrow' methode form ConnectionPool before 'releale'"){assert(counter.map(_._1).indexOf("borrow"))(isLessThan(counter.map(_._1).indexOf("release")))},
            test("Should release borrowed connection"){assert(counter.map(_._2).filter(_.isDefined).headOption.flatten)(equalTo(counter.map(_._3).filter(_.isDefined).headOption.flatten))}
          )
        )
      )
    }
  }.provideLayer(DatabaseSpec.layer ++ CounterMock.layer)
}
