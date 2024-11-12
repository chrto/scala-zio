package zio2demo.model.response

case class AuthenticationResponse(jwt: String)

object AuthenticationResponse {
  import zio.schema.{DeriveSchema, Schema}
  implicit val schema: Schema[AuthenticationResponse] = DeriveSchema.gen[AuthenticationResponse]
}