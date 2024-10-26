package zio2demo.common

object Crypto {
  trait Crypto {
    def hashPwd(pwd: String): String
  }

  object Crypto {
    def hashPwd(pwd: String): String = CryptoLive.live.hashPwd(pwd)
  }

  case class CryptoLive() extends Crypto {
    def hashPwd(password: String) =
      java.security.MessageDigest.getInstance("SHA-256")
      .digest(password.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
  }

  case class CryptoTest() extends Crypto {
    def hashPwd(pwd: String) = s"hashed-$pwd"
  }

  object CryptoLive {
    val live: Crypto = CryptoLive()
    val test: Crypto = CryptoTest()
  }
}