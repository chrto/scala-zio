package zio2demo.model

import zio.uuid.types.UUIDv7

case class Company(id: UUIDv7, name: String) extends Entity
