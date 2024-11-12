package zio2demo.model.body

case class AuthenticationBody(user: String, password: String)

object AuthenticationBody {
  import zio.schema.{DeriveSchema, Schema}
  implicit val schema: Schema[AuthenticationBody] = DeriveSchema.gen[AuthenticationBody]
}