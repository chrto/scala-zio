package zio2demo.web.api.employee

import zio._
import zio.http._
import zio.http.endpoint.{Endpoint, AuthType}
import zio.http.codec.HttpCodec

object EmployeeEndpoints {
  import zio2demo.model.body.EmployeeBody
  import zio2demo.model.response.{CreatedResponse, DeletedResponse, EmployeeResponse}
  import zio2demo.model.ApplicationError._
  import zio2demo.controller.CompanyController

  def get: Endpoint[java.util.UUID, java.util.UUID, ApplicationError, EmployeeResponse, AuthType.None] =
    Endpoint(Method.GET / "api" /  "employees" / zio.http.uuid("employeeId"))
    .out[EmployeeResponse](zio.http.Status.Ok)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def getAll: Endpoint[Unit, Unit, ApplicationError, Vector[EmployeeResponse], AuthType.None] =
    Endpoint(Method.GET / "api" / "employees")
    .out[Vector[EmployeeResponse]](zio.http.Status.Ok)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def insert: Endpoint[Unit, (java.util.UUID, EmployeeBody), ApplicationError, CreatedResponse, AuthType.None] =
    Endpoint(Method.POST / "api" / "employees")
    // CustomCodecs.uuidCodec here
    // .query[java.util.UUID](HttpCodec.query[java.util.UUID]("departmentId"))
    .query[java.util.UUID](HttpCodec.query[String]("departmentId").transformOrFailLeft
        ((uuid: String) =>
          try {
            Right(java.util.UUID.fromString(uuid))
          } catch {
            case _: IllegalArgumentException => Left("Invalid UUID format Chrto")
          }
        )
        ((uuid: java.util.UUID) => uuid.toString())
    )
    // CustomCodecs.body here
    // .in[EmployeeBody]
    .inCodec[EmployeeBody](HttpCodec.content[EmployeeBody].transformOrFailLeft
      {
        case EmployeeBody("", email, password) => Left("Name is empty!")
        case body => Right(body)
      }
      (identity))
    .out[CreatedResponse](zio.http.Status.Created)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[BadRequest](zio.http.Status.BadRequest),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def delete: Endpoint[java.util.UUID, java.util.UUID, ApplicationError, DeletedResponse, AuthType.None] =
    Endpoint(Method.DELETE / "api" / "employees"/ zio.http.uuid("employeeId"))
    .out[DeletedResponse](zio.http.Status.Ok)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )
}