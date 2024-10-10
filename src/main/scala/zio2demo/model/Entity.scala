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
  given EntityType[Car] with {
    def entityName: String = "car"
    def getForeignKey(e: Car): Option[Int] = Some(e.employeeId)
  }
}