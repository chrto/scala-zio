package zio2demo.storage

import zio.{ZIO, UIO, Cause, Scope, ZLayer, URLayer, Ref}

import zio2demo.storage.driver.{ConnectionPool, Connection}
import zio2demo.model.error.DatabaseError
import zio2demo.model.Car

class DB(connectionPool: ConnectionPool) {
  private def connection: ZIO[Scope, DatabaseError, Connection] =
    ZIO.acquireRelease
      (connectionPool.borrow)
      (connectionPool.release)

  def transact[R, E, A](dbProgram: ZIO[Connection & R, E, A]): ZIO[R, E | DatabaseError, A] =
    ZIO.scoped{
      connection.flatMap(connection => dbProgram.provideSomeLayer[R](ZLayer.succeed(connection)))
    }
}

object DB {
  lazy val live: URLayer[ConnectionPool, DB] = ZLayer.fromFunction(new DB(_))
}