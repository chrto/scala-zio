package zio2demo.model

trait Entity {
  val id: Int
}

trait EntityType[E <: Entity] {
  def entityName: String
  def getPrimaryKey(e: E): Int = e.id
  def getForeignKey(e: E): Option[Int]
}

object EntityType {
  given EntityType[Department] with {
    def entityName: String = "department"
    def getForeignKey(e: Department): Option[Int] = Some(e.companyId)
  }

  given EntityType[Employee] with {
    def entityName: String = "employee"
    def getForeignKey(e: Employee): Option[Int] = Some(e.departmentId)
  }

  given EntityType[Car] with {
    def entityName: String = "car"
    def getForeignKey(e: Car): Option[Int] = Some(e.employeeId)
  }
}