package zio2demo.model.error

trait ServiceError

case class EntityExistsError(id: Int) extends ServiceError