package zio2demo.model.error

trait ServiceError

case class LicensePlateExistsError(licencePlate: String) extends ServiceError