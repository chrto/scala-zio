package zio2demo.model.response

case class EmployeeResponse(id: String, name: String, email: String, departmentId: String)

object EmployeeResponse {
  import zio.schema.{DeriveSchema, Schema}
  import zio2demo.model.Employee

  def apply(employee: Employee): EmployeeResponse = EmployeeResponse(employee.id.toString(), employee.name, employee.email, employee.departmentId.toString())
  implicit val schema: Schema[EmployeeResponse] = DeriveSchema.gen[EmployeeResponse]
}
