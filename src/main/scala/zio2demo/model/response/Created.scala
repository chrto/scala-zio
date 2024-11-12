package zio2demo.model.response

case class CreatedResponse(id: String, entityName: String)

object CreatedResponse {
  import zio.schema.{DeriveSchema, Schema}
  implicit val schema: Schema[CreatedResponse] = DeriveSchema.gen[CreatedResponse]
}