package zio2demo.web.middleware.authentication

import zio.http.HandlerAspect
import zio.Config.Secret

object AuthenticationBearer {
  import zio2demo.common.JwtToken._

  val bearerAuth: HandlerAspect[Any, Unit] =
    HandlerAspect.bearerAuth(
      (token: Secret) =>
        JwtToken.jwtDecode(token.value.asString, SECRET_KEY)
          .map(_ => true)
          .getOrElse(false)
    )
}