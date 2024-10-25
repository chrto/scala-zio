package zio2demo.common

object Crypto {
  def hashPwd(pwd: String) =
    java.security.MessageDigest.getInstance("SHA-256")
    .digest("some string".getBytes("UTF-8"))
    .map("%02x".format(_)).mkString
}