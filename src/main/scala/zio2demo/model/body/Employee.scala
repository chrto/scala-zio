package zio2demo.model.body

case class EmployeeBody(name: String, email: String, password: String)

object EmployeeBody {
  import zio.schema.{DeriveSchema, Schema}
  implicit val schema: Schema[EmployeeBody] = DeriveSchema.gen[EmployeeBody]
}
