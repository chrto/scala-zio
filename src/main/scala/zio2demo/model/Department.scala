package zio2demo.model

import zio.uuid.types.UUIDv7

case class Department(id: UUIDv7, name: String, companyId: UUIDv7) extends Entity
