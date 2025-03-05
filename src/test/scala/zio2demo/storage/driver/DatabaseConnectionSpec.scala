package zio2demo.storage.driver

import zio.{ZIO, IO, ULayer, ZLayer, Ref, Exit, Cause}
import zio.test._
import zio.test.Assertion._

object DatabaseConnectionSpec extends ZIOSpecDefault {
  import zio2demo.model.ApplicationError.{ApplicationError, InternalServerError}
  import zio2demo.model.ErrorOrigin
  import zio2demo.model.{Entity}

  // Not necessary layer but
  object KeyValueStoreMock {
    val layer: ULayer[KeyValueStore[ApplicationError, IO]] =
      ZLayer.fromZIO(Ref.make(Map.empty[String, Vector[Entity]]).map(KeyValueStoreLive(_)))
  }

  object ConnectionPoolMock {
    val layer: ZLayer[Any, Nothing, Ref[Vector[ConnectionLive]]] =
      KeyValueStoreMock.layer >>>
        ZLayer.fromFunction((kvl: KeyValueStore[ApplicationError, IO]) => Vector(
          ConnectionLive("connection-1", kvl),
          ConnectionLive("connection-2", kvl),
          ConnectionLive("connection-3", kvl)
        ))
          .flatMap((env: zio.ZEnvironment[Vector[ConnectionLive]]) => ZLayer.fromZIO(Ref.make(env.get[Vector[ConnectionLive]])))
  }

  def spec = suiteAll("Connection Pool") {
    suite("borrow") (
      suite("Pool borrow Initialization")(
        for {
          pool <- ZIO.service[Ref[Vector[ConnectionLive]]].flatMap(_.get)
        } yield zio.Chunk(
          test("Should have 3 available connections"){
            assert(pool)(hasSize(equalTo(3)))
          },
          test("Should have exact connections"){
            assert(pool.map(_.id))(hasSameElements(Vector[String]("connection-1", "connection-2", "connection-3")))
          }
        )
      ),

      suite("Borrow first")(
        for {
          connection <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(_.borrow)
          pool <- ZIO.service[Ref[Vector[ConnectionLive]]].flatMap(_.get)
        } yield zio.Chunk(
          test("Should borrow first connection from pool"){
            assert(connection.id)(equalTo[String]("connection-1"))
          },
          test("Should have 2 available connections"){
            assert(pool)(hasSize(equalTo(2)))
          },
          test("Should have exact connections"){
            assert(pool.map(_.id))(hasSameElements(Vector[String]("connection-2", "connection-3")))
          }
        )
      ),

      suite("Borrow second")(
        for {
          connection <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(_.borrow)
          pool <- ZIO.service[Ref[Vector[ConnectionLive]]].flatMap(_.get)
        } yield zio.Chunk(
          test("Should borrow second connection from pool"){
            assert(connection.id)(equalTo[String]("connection-2"))
          },
          test("Should have 1 available connections"){
            assert(pool)(hasSize(equalTo(1)))
          },
          test("Should have exact connections"){
            assert(pool.map(_.id))(hasSameElements(Vector[String]("connection-3")))
          }
        )
      ),

      suite("Borrow third")(
        for {
          connection <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(_.borrow)
          pool <- ZIO.service[Ref[Vector[ConnectionLive]]].flatMap(_.get)
        } yield zio.Chunk(
          test("Should borrow third connection from pool"){
            assert(connection.id)(equalTo[String]("connection-3"))
          },
          test("Should have no available connections"){
            assert(pool)(isEmpty)
          }
        )
      ),

      suite("Borrow fourth")(
        for {
          connection <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(_.borrow).exit
        } yield zio.Chunk(
          test("Should fail with exact ApplicationError"){
            assertTrue(connection match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(InternalServerError("No connections available", ErrorOrigin.DatabaseError())))
            )
          }
        )
      )
    ).provideShared(ConnectionPoolMock.layer ++ KeyValueStoreMock.layer) @@ TestAspect.sequential

    suite("release")(
      suite("release borrowed connection")(
        for {
          connections <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(pool => pool.borrow <*> pool.borrow <*> pool.borrow)
          _ <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(_.release(connections._2))
          pool <- ZIO.service[Ref[Vector[ConnectionLive]]].flatMap(_.get)
        } yield {
          zio.Chunk(
            test("Should borrow first connection"){assert(connections._1.id)(equalTo("connection-1"))},
            test("Should borrow second connection"){assert(connections._2.id)(equalTo("connection-2"))},
            test("Should borrow third connection"){assert(connections._3.id)(equalTo("connection-3"))},
            test("Should release one connection after has been borrowed"){assert(pool)(hasSize[ConnectionLive](equalTo(1)))},
            test("Should release second connection after has been borrowed"){assert(pool.map(_.id))(hasSameElements(Vector("connection-2")))}
          )
        }
      ),

      suite("release borrowed connection twice")(
        for {
          connections <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(pool => pool.borrow <*> pool.borrow <*> pool.borrow)
          _ <- ZIO.service[Ref[Vector[ConnectionLive]]].map(ConnectionPoolLive(_)).flatMap(pool =>
              pool.release(connections._2) *> pool.release(connections._2) *> pool.release(connections._2)
            )
          pool <- ZIO.service[Ref[Vector[ConnectionLive]]].flatMap(_.get)
        } yield {
          zio.Chunk(
            test("Should borrow first connection"){assert(connections._1.id)(equalTo("connection-1"))},
            test("Should borrow second connection"){assert(connections._2.id)(equalTo("connection-2"))},
            test("Should borrow third connection"){assert(connections._3.id)(equalTo("connection-3"))},
            test("Should release one connection after has been borrowed"){assert(pool)(hasSize[ConnectionLive](equalTo(1)))},
            test("Should release second connection after has been borrowed"){assert(pool.map(_.id))(hasSameElements(Vector("connection-2")))}
          )
        }
      )
    ).provide(ConnectionPoolMock.layer ++ KeyValueStoreMock.layer)

  }
}