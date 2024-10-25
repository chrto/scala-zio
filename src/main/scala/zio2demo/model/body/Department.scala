package zio2demo.model.body

case class DepartmentBody(name: String)

object DepartmentBody {
  import zio.schema.{DeriveSchema, Schema}

  implicit val schema: Schema[DepartmentBody] = DeriveSchema.gen[DepartmentBody]
}
