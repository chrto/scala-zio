package zio2demo.storage.driver

import zio.{Ref, UIO, IO, ZIO, ULayer, ZLayer, ZEnvironment}
import zio2demo.model.ApplicationError._
import zio2demo.model.ErrorOrigin

trait ConnectionPool {
  def borrow: IO[ApplicationError, Connection]
  def release(c: Connection): UIO[Unit]
}

case class ConnectionPoolLive(ref: Ref[Vector[Connection]]) extends ConnectionPool {
  def borrow: IO[ApplicationError, Connection] =
    ref.modify {
      case head +: tail => (Some(head), tail)
      case v            => (None, v)
    }
      .flatMap {
        case Some(connection) => ZIO.succeed(connection)
        case None             => ZIO.fail(InternalServerError("No connections available", ErrorOrigin.DatabaseError()))
      }
      .tap((connection: Connection) => ZIO.logDebug(s"Obtained connection with id: ${connection.id}") )

  def release(c: Connection): UIO[Unit] =
    ref.update(pool =>
      pool.map(_.id).contains(c.id) match
        case true => pool
        case false => pool :+ c
    )
      .tap(_ => ZIO.logDebug(s"Released connection with id: ${c.id}"))
}

object ConnectionPoolLive {
  val live: ULayer[ConnectionPool] = KeyValueStoreLive.live
    >>> ZLayer.fromFunction((kvl: KeyValueStore[ApplicationError, IO]) => Vector(
      ConnectionLive("connection-1", kvl),
      ConnectionLive("connection-2", kvl),
      ConnectionLive("connection-3", kvl)
    ))
    .flatMap((env: zio.ZEnvironment[Vector[Connection]])=> ZLayer.fromZIO(Ref.make(env.get[Vector[Connection]])))
    >>> ZLayer.fromFunction(ConnectionPoolLive(_))
}
