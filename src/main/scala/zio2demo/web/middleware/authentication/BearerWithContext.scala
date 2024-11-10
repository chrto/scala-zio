package zio2demo.web.middleware.authentication

import zio.ZIO
import zio.http.{HandlerAspect, Handler, Response, Request, Header, Headers}
import zio.json.EncoderOps
import zio.uuid.types.UUIDv7

import zio2demo.model.Employee
import zio2demo.service.EmployeeService

trait AuthenticationBearerWithContext {
  val bearerAuth: HandlerAspect[EmployeeService, Employee]
}

object AuthenticationBearerWithContext extends AuthenticationBearerWithContext {
  import zio2demo.model.ApplicationError._
  import zio2demo.common.JwtToken._

  def buildResponse(e: ApplicationError): Response = Response.json(e.toJson).status(zio.http.Status.Unauthorized)

  val bearerAuth: HandlerAspect[EmployeeService, Employee] =
    HandlerAspect.interceptIncomingHandler(Handler.fromFunctionZIO[Request] { request =>
      request.header(Header.Authorization) match {
        case Some(Header.Authorization.Bearer(token)) =>
          ZIO.fromEither(
            JwtToken.jwtDecode(token.value.asString, SECRET_KEY)
            .left.map(buildResponse)
            .flatMap(_.subject.toRight(buildResponse(Unauthenticated("Missing subject claim!"))))
          )
          .map(java.util.UUID.fromString)
          .map(UUIDv7.wrap)
          .flatMap(EmployeeService.get)
          .map(request -> _)
          .catchAll{
            case appErr: ApplicationError => ZIO.fail(buildResponse(appErr))
            case res: Response => ZIO.fail(res)
          }
        case _ => ZIO.fail(Response.json(Unauthenticated("Missing authorization header!").toJson).status(zio.http.Status.Unauthorized).addHeaders(Headers(Header.WWWAuthenticate.Bearer(realm = "Access"))))
      }
    })
}
