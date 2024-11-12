package zio2demo.model

import zio.uuid.types.UUIDv7

trait Entity {
  val id: UUIDv7
}

trait EntityType[E <: Entity] {
  def entityName: String
  def getPrimaryKey(e: E): UUIDv7 = e.id
  def getForeignKey(e: E): Option[UUIDv7]
}

object EntityType {
  given EntityType[Company] with {
    def entityName: String = "companies"
    def getForeignKey(e: Company): Option[UUIDv7] = None
  }

  given EntityType[Department] with {
    def entityName: String = "departments"
    def getForeignKey(e: Department): Option[UUIDv7] = Some(e.companyId)
  }

  given EntityType[Employee] with {
    def entityName: String = "employees"
    def getForeignKey(e: Employee): Option[UUIDv7] = Some(e.departmentId)
  }
}