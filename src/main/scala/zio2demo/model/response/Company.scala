package zio2demo.model.response

case class CompanyResponse(id: String, name: String)

object CompanyResponse {
  def apply(company: zio2demo.model.Company): CompanyResponse = CompanyResponse(company.id.toString(), company.name)

  import zio.schema.{DeriveSchema, Schema}
  implicit val schema: Schema[CompanyResponse] = DeriveSchema.gen[CompanyResponse]
}