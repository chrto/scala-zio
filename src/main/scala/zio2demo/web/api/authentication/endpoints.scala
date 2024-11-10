package zio2demo.web.api.authentication

import zio._
import zio.http._
import zio.http.endpoint.{Endpoint, AuthType}
import zio.http.codec.HttpCodec

object AuthenticationEndpoints {
  import zio2demo.model.body.AuthenticationBody
  import zio2demo.model.response.AuthenticationResponse
  import zio2demo.model.ApplicationError._

  def authentication: Endpoint[Unit, AuthenticationBody, ApplicationError, AuthenticationResponse, AuthType.None] =
    Endpoint(Method.POST / "api" / "authentication")
    .in[AuthenticationBody]
    .out[AuthenticationResponse](zio.http.Status.Ok)
    .outErrors[ApplicationError](
      HttpCodec.error[NotFound](zio.http.Status.NotFound),
      HttpCodec.error[Unauthenticated](zio.http.Status.Forbidden),
      HttpCodec.error[BadRequest](zio.http.Status.BadRequest),
      HttpCodec.error[Forbidden](zio.http.Status.Forbidden),
      HttpCodec.error[InternalServerError](zio.http.Status.InternalServerError)
    )
}