package zio2demo.web.api.company

import zio._
import zio.http._
import zio.http.endpoint.{Endpoint, AuthType}
import zio.http.codec.HttpCodec

object CompanyEndpoints {
  import zio2demo.model.body.CompanyBody
  import zio2demo.model.response.{CreatedResponse, DeletedResponse, CompanyResponse}
  import zio2demo.model.ApplicationError._
  import zio2demo.controller.CompanyController

  def get: Endpoint[java.util.UUID, java.util.UUID, ApplicationError, CompanyResponse, AuthType.None] =
    Endpoint(Method.GET / "api" /  "companies" / zio.http.uuid("companyId"))
    .out[CompanyResponse](zio.http.Status.Ok)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def getAll: Endpoint[Unit, Unit, ApplicationError, Vector[CompanyResponse], AuthType.None] =
    Endpoint(Method.GET / "api" / "companies")
    .out[Vector[CompanyResponse]](zio.http.Status.Ok)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def insert: Endpoint[Unit, CompanyBody, ApplicationError, CreatedResponse, AuthType.None] =
    Endpoint(Method.POST / "api" / "companies")
    .in[CompanyBody]
    .out[CreatedResponse](zio.http.Status.Created)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[BadRequest](zio.http.Status.BadRequest),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )

  def delete: Endpoint[java.util.UUID, java.util.UUID, ApplicationError, DeletedResponse, AuthType.None] =
    Endpoint(Method.DELETE / "api" / "companies"/ zio.http.uuid("companyId"))
    .out[DeletedResponse](zio.http.Status.Ok)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )
}