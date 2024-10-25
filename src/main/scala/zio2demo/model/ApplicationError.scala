package zio2demo.model

import zio.schema.{DeriveSchema, Schema, TypeId}
import zio.Chunk

object ApplicationError {
  import ErrorOrigin._

  trait ApplicationError

  case class NotFound(msg: String) extends ApplicationError
  case class BadRequest(msg: String) extends ApplicationError
  case class Unauthorized(msg: String) extends ApplicationError
  case class Forbidden(msg: String) extends ApplicationError
  case class InternalServerError(msg: String, origin: ErrorOrigin) extends ApplicationError

  implicit val notFoundSchema: Schema[NotFound] = DeriveSchema.gen[NotFound]
  implicit val badRequestSchema: Schema[BadRequest] = DeriveSchema.gen[BadRequest]
  implicit val unauthorizedSchema: Schema[Unauthorized] = DeriveSchema.gen[Unauthorized]
  implicit val forbiddenSchema: Schema[Forbidden] = DeriveSchema.gen[Forbidden]
  implicit val internalServerErrorSchema: Schema[InternalServerError] = DeriveSchema.gen[InternalServerError]

  implicit val errorOriginSchema: Schema[ErrorOrigin] = Schema.Enum3[DatabaseError, ServiceError, ControllerError, ErrorOrigin](
      id = TypeId.fromTypeName("ErrorOrigin"),
      case1 = Schema.Case[ErrorOrigin, DatabaseError](
        id = "DatabaseError",
        schema = DatabaseError.schema,
        unsafeDeconstruct = pm => pm.asInstanceOf[DatabaseError],
        construct = cc => cc.asInstanceOf[ErrorOrigin],
        isCase = _.isInstanceOf[DatabaseError],
        annotations = Chunk.empty
      ),
      case2 = Schema.Case[ErrorOrigin, ServiceError](
        id = "ServiceError",
        schema = ServiceError.schema,
        unsafeDeconstruct = pm => pm.asInstanceOf[ServiceError],
        construct = wt => wt.asInstanceOf[ErrorOrigin],
        isCase = _.isInstanceOf[ServiceError],
        annotations = Chunk.empty
      ),
      case3 = Schema.Case[ErrorOrigin, ControllerError](
        id = "ControllerError",
        schema = ControllerError.schema,
        unsafeDeconstruct = pm => pm.asInstanceOf[ControllerError],
        construct = wt => wt.asInstanceOf[ErrorOrigin],
        isCase = _.isInstanceOf[ControllerError],
        annotations = Chunk.empty
      )
  )
}

object ErrorOrigin {
  trait ErrorOrigin
  final case class DatabaseError() extends ErrorOrigin
  object DatabaseError {
    implicit val schema: Schema[DatabaseError] = DeriveSchema.gen[DatabaseError]
  }

  final case class ServiceError() extends ErrorOrigin
  object ServiceError {
    implicit val schema: Schema[ServiceError] = DeriveSchema.gen[ServiceError]
  }

  final case class ControllerError() extends ErrorOrigin
  object ControllerError {
    implicit val schema: Schema[ControllerError] = DeriveSchema.gen[ControllerError]
  }
}