package zio2demo.storage

import zio.{ZIO, Scope, ZLayer, URLayer, ULayer, Tag}

import zio2demo.storage.driver.{ConnectionPool, Connection}
import zio2demo.model.ApplicationError._
import zio2demo.storage.driver.ConnectionPoolLive

trait Database {
  def transact[R, A: Tag](dbProgram: ZIO[Connection & R, ApplicationError, A]): ZIO[R, ApplicationError, A]
}

case class DatabaseLive(connectionPool: ConnectionPool) extends Database {
  private def connection: ZIO[Scope, ApplicationError, Connection] =
    ZIO.acquireRelease
      (connectionPool.borrow)
      (connectionPool.release)

  def transact[R, A: Tag](dbProgram: ZIO[Connection & R, ApplicationError, A]): ZIO[R, ApplicationError, A] =
    ZIO.scoped{
      connection.flatMap(connection => dbProgram.provideSomeLayer[R](ZLayer.succeed(connection)))
    }
}

object DatabaseLive {
  lazy val live: URLayer[ConnectionPool, Database] = ZLayer.fromFunction(new DatabaseLive(_))
  lazy val liveWithPool: ULayer[Database] = ConnectionPoolLive.live >>> ZLayer.fromFunction(new DatabaseLive(_))
}
