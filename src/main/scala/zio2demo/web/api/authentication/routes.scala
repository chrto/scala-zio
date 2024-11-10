package zio2demo.web.api.authentication

import zio._
import zio.http._
import zio.uuid.UUIDGenerator
import zio.uuid.types.UUIDv7

object AuthenticationRoutes {
  import zio2demo.model.response.AuthenticationResponse
  import zio2demo.model.Employee
  import zio2demo.model.body.AuthenticationBody
  import zio2demo.model.ApplicationError._
  import zio2demo.service.EmployeeService
  import zio2demo.web.api.authentication.AuthenticationEndpoints
  import zio2demo.common.Crypto.Crypto
  import zio2demo.common.JwtToken._

  private def handleAuthentication(payload: AuthenticationBody): ZIO[EmployeeService, ApplicationError, AuthenticationResponse] =
    ZIO.logSpan("authentication") {
      EmployeeService.getByCredentials(payload.user, Crypto.hashPwd(payload.password))
        .map((employee: Employee) => AuthenticationResponse(JwtToken.jwtEncode(employee.id.toString(), SECRET_KEY)))
        .catchAll{
          case appError: Unauthenticated => ZIO.fail(appError)
          case err => ZIO.fail(Unauthenticated("Invalid credentials!"))
        }
    }

  def make: Routes[EmployeeService, Nothing] = Routes(
    AuthenticationEndpoints.authentication.implementHandler(handler(handleAuthentication)),
    // AuthenticationEndpoints.authentication.implement(handleAuthentication)  // ^ same as above ^
  )
}
