package zio2demo

// import zio.{ZIOAppArgs, ZIO, ZLayer, ZIOAppDefault, Scope, ULayer, URIO}
import zio.{ZIOAppDefault}
import  zio.http.Server

object MyHttpApp extends ZIOAppDefault {
  import zio2demo.web.app.WebAppRoutes
  override val run =
    zio.Console.printLine("please visit page http://localhost:8080") *>
      Server.serve(WebAppRoutes.routes).provide(Server.default)
}

