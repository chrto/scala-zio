package zio2demo.model

import zio.uuid.types.UUIDv7

case class Employee(id: UUIDv7, name: String, email: String, pwdHash: String, departmentId: UUIDv7) extends Entity
