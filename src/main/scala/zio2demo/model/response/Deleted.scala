package zio2demo.model.response

case class DeletedResponse(id: String, entityName: String)

object DeletedResponse {
  import zio.schema.{DeriveSchema, Schema}
  implicit val schema: Schema[DeletedResponse] = DeriveSchema.gen[DeletedResponse]
}