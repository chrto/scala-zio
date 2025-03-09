package zio2demo.storage.driver

import zio.{ZIO, IO, UIO, ULayer, ZLayer, Ref, Exit, Cause}
import zio.test._
import zio.test.Assertion._
import cats.syntax.applicative._

object DatabaseConnectionSpec extends ZIOSpecDefault {
  import zio2demo.model.ApplicationError.{ApplicationError, InternalServerError}
  import zio2demo.model.ErrorOrigin
  import zio2demo.model.{Entity}

  // Not necessary layer but
  object KeyValueStoreMock {
    val layer: ULayer[KeyValueStore[ApplicationError, IO]] =
      ZLayer.fromZIO(Ref.make(Map.empty[String, Vector[Entity]]).map(KeyValueStoreLive(_)))
  }

  class ConnectionsBorrowed(connections: Ref[Vector[Connection]]) {
    def add(connection: Connection): UIO[Unit] =
      connections.update(pool =>
        pool.map(_.id).contains(connection.id) match
          case true => pool
          case false => pool :+ connection
      )

    def add(cs: Vector[Connection]): UIO[Unit] =
      cs.foldLeft(ZIO.succeed[Unit](connections.get))((acc, c) => acc *> add(c))

    def get(id: String): UIO[Option[Connection]] = connections.modify(pool =>
      pool.find(_.id == id) match
        case None => (None, pool)
        case Some(value) => (value.pure[Option], pool.filterNot(_.id == id))
    )

    def size: UIO[Int] = connections.get.map(_.length)

    def read(id: String): UIO[Option[Connection]] = connections.get.map(_.find(_.id == id))
    def readPool: UIO[Vector[Connection]] = connections.get
  }

  object ConnectionsBorrowed {
    val layer: ZLayer[Any, Nothing, ConnectionsBorrowed] =
      ZLayer.fromZIO(Ref.make(Vector.empty[Connection]).map(ConnectionsBorrowed(_)))
  }

  object ConnectionPoolMock {
    val layer: ZLayer[Any, Nothing, ConnectionPoolLive] =
      KeyValueStoreMock.layer >>>
        ZLayer.fromFunction((kvl: KeyValueStore[ApplicationError, IO]) => Vector(
          ConnectionLive("connection-1", kvl),
          ConnectionLive("connection-2", kvl),
          ConnectionLive("connection-3", kvl)
        ))
          .flatMap((env: zio.ZEnvironment[Vector[Connection]]) => ZLayer.fromZIO(Ref.make(env.get[Vector[Connection]]))) >>>
          ZLayer.fromFunction(ConnectionPoolLive(_))
  }

  def spec = suiteAll("Connection Pool") {
    suite("borrow") (
      suite("Pool borrow Initialization")(
        for {
          pool <- ZIO.service[ConnectionPoolLive].flatMap(_.ref.get)
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
          connection <- ZIO.service[ConnectionPoolLive].flatMap(_.borrow)
          pool <- ZIO.service[ConnectionPoolLive].flatMap(_.ref.get)
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
          connection <- ZIO.service[ConnectionPoolLive].flatMap(_.borrow)
          pool <- ZIO.service[ConnectionPoolLive].flatMap(_.ref.get)
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
          connection <- ZIO.service[ConnectionPoolLive].flatMap(_.borrow)
          pool <- ZIO.service[ConnectionPoolLive].flatMap(_.ref.get)
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
          connection <- ZIO.service[ConnectionPoolLive].flatMap(_.borrow).exit
        } yield zio.Chunk(
          test("Should fail with exact ApplicationError"){
            assertTrue(connection match
              case Exit.Success(_) => false
              case Exit.Failure(cause) => cause.contains(Cause.fail(InternalServerError("No connections available", ErrorOrigin.DatabaseError())))
            )
          }
        )
      )
    ).provideShared(ConnectionPoolMock.layer) @@ TestAspect.sequential

    suite("release")(
      suite("Borrow all connections")(
        for {
          connections <- ZIO.service[ConnectionPoolLive].flatMap(pool => pool.borrow <*> pool.borrow <*> pool.borrow)
          _ <- ZIO.service[ConnectionsBorrowed].flatMap(_.add(Vector.empty[Connection] :+ connections._1 :+ connections._2 :+ connections._3))

          borrowed <- ZIO.service[ConnectionsBorrowed].flatMap(
              connections => connections.read("connection-1") <*> connections.read("connection-2") <*> connections.read("connection-3")
            )
          borrowedSize <- ZIO.service[ConnectionsBorrowed].flatMap(_.size)
          pool <- ZIO.service[ConnectionPoolLive].flatMap(_.ref.get)
        } yield {
          zio.Chunk(
            suite("Connection Pool")(
              test("Should be empty"){assert(pool)(isEmpty)}
            ),
            suite("Connection Borrowed")(
              test("Should has exact size"){assert(borrowedSize)(equalTo(3))},
              test("Should borrow first connection"){assert(borrowed._1)(isSome)},
              test("Should borrow second connection"){assert(borrowed._2)(isSome)},
              test("Should borrow third connection"){assert(borrowed._3)(isSome)}
            )
          )
        }
      ),

      suite("release borowed connection") (
        for {
          connection <- ZIO.service[ConnectionsBorrowed].flatMap(_.get("connection-1"))
          _ <- ZIO.service[ConnectionPoolLive].flatMap(_.release(connection.get))

          borrowed <- ZIO.service[ConnectionsBorrowed].flatMap(
            connections => connections.read("connection-1") <*> connections.read("connection-2") <*> connections.read("connection-3")
          )
          borrowedSize <- ZIO.service[ConnectionsBorrowed].flatMap(_.size)
          pool <- ZIO.service[ConnectionPoolLive].flatMap(_.ref.get)
        } yield {
          zio.Chunk(
            suite("Connection Pool")(
              test("Should have exact one connection"){assert(pool)(hasSize(equalTo(1)))},
              test("Should have first connection"){assert(pool.headOption.map(_.id))(isSome(equalTo("connection-1")))}
            ),
            suite("Connection Borrowed")(
              test("Should has exact size"){assert(borrowedSize)(equalTo(2))},
              test("Should release firts connection"){assert(borrowed._1)(isNone)},
              test("Should borrow second connection"){assert(borrowed._2)(isSome)},
              test("Should borrow third connection"){assert(borrowed._3)(isSome)}
            )
          )
        }
      ),

      suite("release borrowed connection twice")(
        for {
          connection <- ZIO.service[ConnectionsBorrowed].flatMap(_.get("connection-2"))
          _ <- ZIO.service[ConnectionPoolLive].flatMap(
              pool => pool.release(connection.get) *> pool.release(connection.get) *> pool.release(connection.get)
            )

          borrowed <- ZIO.service[ConnectionsBorrowed].flatMap(
            connections => connections.read("connection-1") <*> connections.read("connection-2") <*> connections.read("connection-3")
          )
          borrowedSize <- ZIO.service[ConnectionsBorrowed].flatMap(_.size)
          pool <- ZIO.service[ConnectionPoolLive].flatMap(_.ref.get)
        } yield {
          zio.Chunk(
            suite("Connection Pool")(
              test("Should have exact one connection"){assert(pool)(hasSize(equalTo(2)))},
              test("Should have first connection"){assert(pool.map(_.id).find(_ == "connection-1"))(isSome)},
              test("Should have second connection"){assert(pool.map(_.id).find(_ == "connection-2"))(isSome)}
            ),
            suite("Connection Borrowed")(
              test("Should has exact size"){assert(borrowedSize)(equalTo(1))},
              test("Should release firts connection"){assert(borrowed._1)(isNone)},
              test("Should release second connection"){assert(borrowed._2)(isNone)},
              test("Should borrow third connection"){assert(borrowed._3)(isSome)}
            )
          )
        }
      )
    ).provideShared(ConnectionPoolMock.layer ++ ConnectionsBorrowed.layer) @@ TestAspect.sequential
  } @@ TestAspect.parallel
}