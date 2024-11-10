package zio2demo.common

import pdi.jwt.{JwtClaim, Jwt, JwtAlgorithm}
import pdi.jwt.exceptions.JwtException
import java.time.Clock

object JwtToken {
  import zio2demo.model.ApplicationError.{ApplicationError, InternalServerError, Unauthenticated}
  import zio2demo.model.ErrorOrigin

  implicit val clock: Clock = Clock.systemUTC

  val SECRET_KEY = "sdjfohfp87s6df897dsatfh"  // TODO: move to config

  trait JwtToken {
    def jwtEncode(userId: String, secretKey: String): String
    def jwtDecode(token: String, key: String): Either[ApplicationError, JwtClaim]
  }

  object JwtToken {
    def jwtEncode(userId: String, secretKey: String): String =
      JwtTokenLive.jwtEncode(userId, secretKey)

    def jwtDecode(token: String, secretKey: String): Either[ApplicationError, JwtClaim] =
      JwtTokenLive.jwtDecode(token, secretKey)
  }

  object JwtTokenLive extends JwtToken {
    def jwtEncode(userId: String, secretKey: String): String =
      Jwt.encode(
        JwtClaim()
          .about(userId)
          .by("zio2demo")
          .issuedNow
          .expiresIn(30000000),
        secretKey,
        JwtAlgorithm.HS512
      )

    def jwtDecode(token: String, secretKey: String): Either[ApplicationError, JwtClaim] =
        Jwt.decode(token, secretKey, Seq(JwtAlgorithm.HS512))
        .toEither
        .left.map{
          case e: JwtException => Unauthenticated(s"Invalid token: ${e.getMessage}")
          case e => InternalServerError(s"Authentication failed: ${e.getMessage}", ErrorOrigin.AuthenticationError())
        }
  }
}