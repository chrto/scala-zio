package zio2demo.model.body

case class CompanyBody(name: String)

object CompanyBody {
  import zio.schema.{DeriveSchema, Schema}
  implicit val schema: Schema[CompanyBody] = DeriveSchema.gen[CompanyBody]
}