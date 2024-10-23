package zio2demo.model.error

trait DatabaseError

case object ConnectionNotAvailable extends DatabaseError
case class NotFound(msg: String) extends DatabaseError