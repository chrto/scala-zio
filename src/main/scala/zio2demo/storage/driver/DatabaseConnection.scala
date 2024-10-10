package zio2demo.storage.driver

import zio.{Ref, UIO, IO, ZIO, ULayer, ZLayer, ZEnvironment}
import zio2demo.model.error.{DatabaseError, ConnectionNotAvailable}

trait ConnectionPool {
  def borrow: IO[DatabaseError, Connection]
  def release(c: Connection): UIO[Unit]
}

case class ConnectionPoolLive(ref: Ref[Vector[Connection]]) extends ConnectionPool {
  def borrow: IO[DatabaseError, Connection] =
    ref.modify {
      case head +: tail => (Some(head), tail)
      case v            => (None, v)
    }
      .flatMap {
        case Some(connection) => ZIO.succeed(connection)
        case None             => ZIO.fail(ConnectionNotAvailable)
      }
      .tap((connection: Connection) => ZIO.logInfo(s"Obtained connection with id: ${connection.id}") )

  def release(c: Connection): UIO[Unit] =
    ref.update(_ :+ c)
      .tap(_ => ZIO.logInfo(s"Released connection with id: ${c.id}"))
}

object ConnectionPoolLive {
  val live: ULayer[ConnectionPool] = KeyValueStoreLive.live
    >>> ZLayer.fromFunction((kvl: KeyValueStore[DatabaseError, IO]) => Vector(
      Connection("connection-1", kvl),
      Connection("connection-2", kvl),
      Connection("connection-3", kvl)
    ))
    .flatMap((env: zio.ZEnvironment[Vector[Connection]])=> ZLayer.fromZIO(Ref.make(env.get[Vector[Connection]])))
    >>> ZLayer.fromFunction(ConnectionPoolLive(_))
}
