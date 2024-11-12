package zio2demo.web.api.department

import zio._
import zio.http._
import zio.http.endpoint.{Endpoint, AuthType}
import zio.http.codec.HttpCodec

object DepartmentEndpoints {
  import zio2demo.model.body.DepartmentBody
  import zio2demo.model.response.{CreatedResponse, DeletedResponse, DepartmentResponse}
  import zio2demo.model.ApplicationError._
  import zio2demo.controller.DepartmentController

  def get: Endpoint[java.util.UUID, java.util.UUID, ApplicationError, DepartmentResponse, AuthType.Bearer] =
    Endpoint(Method.GET / "api" /  "departments" / zio.http.uuid("departmentId"))
    .out[DepartmentResponse](zio.http.Status.Ok)
    .auth(AuthType.Bearer)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def getAll: Endpoint[Unit, Unit, ApplicationError, Vector[DepartmentResponse], AuthType.Bearer] =
    Endpoint(Method.GET / "api" / "departments")
    .out[Vector[DepartmentResponse]](zio.http.Status.Ok)
    .auth(AuthType.Bearer)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def insert: Endpoint[Unit, (java.util.UUID, DepartmentBody), ApplicationError, CreatedResponse, AuthType.Bearer] =
    Endpoint(Method.POST / "api" / "departments")
    .query[java.util.UUID](HttpCodec.query[java.util.UUID]("companyId"))
    .in[DepartmentBody]
    .out[CreatedResponse](zio.http.Status.Created)
    .auth(AuthType.Bearer)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[BadRequest](zio.http.Status.BadRequest),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def delete: Endpoint[java.util.UUID, java.util.UUID, ApplicationError, DeletedResponse, AuthType.Bearer] =
    Endpoint(Method.DELETE / "api" / "departments"/ zio.http.uuid("employeeId"))
    .out[DeletedResponse](zio.http.Status.Ok)
    .auth(AuthType.Bearer)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )
}