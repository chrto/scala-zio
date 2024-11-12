package zio2demo.model

import zio.schema.{DeriveSchema, Schema, TypeId}
import zio.Chunk
import zio.json._

object ApplicationError {
  import ErrorOrigin._

  @jsonDiscriminator("type") sealed trait ApplicationError

  implicit val encoder: JsonEncoder[ApplicationError] = DeriveJsonEncoder.gen[ApplicationError]
  implicit val decoder: JsonDecoder[ApplicationError] = DeriveJsonDecoder.gen[ApplicationError]

  case class NotFound(message: String) extends ApplicationError
  implicit val notFoundSchema: Schema[NotFound] = DeriveSchema.gen[NotFound]
  implicit val notFoundEncoder: JsonEncoder[NotFound] = DeriveJsonEncoder.gen[NotFound]
  implicit val notFoundDecoder: JsonDecoder[NotFound] = DeriveJsonDecoder.gen[NotFound]

  case class BadRequest(message: String) extends ApplicationError
  implicit val badRequestSchema: Schema[BadRequest] = DeriveSchema.gen[BadRequest]
  implicit val badRequestEncoder: JsonEncoder[BadRequest] = DeriveJsonEncoder.gen[BadRequest]
  implicit val badRequestDecoder: JsonDecoder[BadRequest] = DeriveJsonDecoder.gen[BadRequest]

  case class Unauthenticated(message: String) extends ApplicationError
  implicit val unauthenticatedSchema: Schema[Unauthenticated] = DeriveSchema.gen[Unauthenticated]
  implicit val unauthenticatedEncoder: JsonEncoder[Unauthenticated] = DeriveJsonEncoder.gen[Unauthenticated]
  implicit val unauthenticatedDecoder: JsonDecoder[Unauthenticated] = DeriveJsonDecoder.gen[Unauthenticated]


  case class Unauthorized(message: String) extends ApplicationError
  implicit val unauthorizedSchema: Schema[Unauthorized] = DeriveSchema.gen[Unauthorized]
  implicit val unauthorizedEncoder: JsonEncoder[Unauthorized] = DeriveJsonEncoder.gen[Unauthorized]
  implicit val unauthorizedDecoder: JsonDecoder[Unauthorized] = DeriveJsonDecoder.gen[Unauthorized]

  case class Forbidden(message: String) extends ApplicationError
  implicit val forbiddenSchema: Schema[Forbidden] = DeriveSchema.gen[Forbidden]
  implicit val forbiddenEncoder: JsonEncoder[Forbidden] = DeriveJsonEncoder.gen[Forbidden]
  implicit val forbiddenDecoder: JsonDecoder[Forbidden] = DeriveJsonDecoder.gen[Forbidden]

  case class InternalServerError(message: String, origin: ErrorOrigin) extends ApplicationError
  implicit val internalServerErrorSchema: Schema[InternalServerError] = DeriveSchema.gen[InternalServerError]
  implicit val internalServerEncoder: JsonEncoder[InternalServerError] = DeriveJsonEncoder.gen[InternalServerError]
  implicit val internalServerDecoder: JsonDecoder[InternalServerError] = DeriveJsonDecoder.gen[InternalServerError]
}

object ErrorOrigin {
  sealed trait ErrorOrigin

  implicit val errorOriginEncoder: JsonEncoder[ErrorOrigin] = DeriveJsonEncoder.gen[ErrorOrigin]
  implicit val errorOriginDecoder: JsonDecoder[ErrorOrigin] = DeriveJsonDecoder.gen[ErrorOrigin]

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

  final case class AuthenticationError() extends ErrorOrigin
  object AuthenticationError {
    implicit val schema: Schema[AuthenticationError] = DeriveSchema.gen[AuthenticationError]
  }
}