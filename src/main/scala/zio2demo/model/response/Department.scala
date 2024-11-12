package zio2demo.model.response


case class DepartmentResponse(id: String, name: String, companyId: String)

object DepartmentResponse {
  import zio.schema.{DeriveSchema, Schema}
  import zio2demo.model.Department

  def apply(department: Department): DepartmentResponse = DepartmentResponse(department.id.toString(), department.name, department.companyId.toString())
  implicit val schema: Schema[DepartmentResponse] = DeriveSchema.gen[DepartmentResponse]
}