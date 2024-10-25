package zio2demo.storage

import zio.{ZIO, UIO, Cause, Scope, ZLayer, URLayer, Ref}

import zio2demo.storage.driver.{ConnectionPool, Connection}
import zio2demo.model.ApplicationError._

trait Database {
  def transact[R, E, A](dbProgram: ZIO[Connection & R, E, A]): ZIO[R, E | ApplicationError, A]
}

case class DatabaseLive(connectionPool: ConnectionPool) extends Database {
  private def connection: ZIO[Scope, ApplicationError, Connection] =
    ZIO.acquireRelease
      (connectionPool.borrow)
      (connectionPool.release)

  def transact[R, E, A](dbProgram: ZIO[Connection & R, E, A]): ZIO[R, E | ApplicationError, A] =
    ZIO.scoped{
      connection.flatMap(connection => dbProgram.provideSomeLayer[R](ZLayer.succeed(connection)))
    }
}

object DatabaseLive {
  lazy val live: URLayer[ConnectionPool, Database] = ZLayer.fromFunction(new DatabaseLive(_))
}