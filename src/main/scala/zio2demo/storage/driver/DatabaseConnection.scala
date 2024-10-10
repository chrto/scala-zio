package zio2demo.storage.driver

import zio.{Ref, UIO, IO, ZIO, ULayer}
import zio.Exit.{Success, Failure}
import zio.ZLayer

import zio2demo.storage.driver.Connection
import zio2demo.model.error.{DatabaseError, ConnectionNotAvailable}
import zio2demo.model.Car

class ConnectionPool(ref: Ref[Vector[Connection]]) {
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

object ConnectionPool {
  private val store: UIO[Ref[Vector[Connection]]]= Ref.make(Vector.empty[Car])
    .flatMap(store => Ref.make(Vector(
      Connection("connection-1", store),
      Connection("connection-2", store),
      Connection("connection-3", store))
    ))
  lazy val live: ULayer[ConnectionPool] = ZLayer.fromZIO(store) >>> ZLayer.fromFunction(new ConnectionPool(_))
}
